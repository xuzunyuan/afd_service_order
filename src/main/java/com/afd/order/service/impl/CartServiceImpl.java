package com.afd.order.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.afd.model.product.BrandShow;
import com.afd.model.product.BrandShowDetail;
import com.afd.model.product.Product;
import com.afd.model.product.Sku;
import com.afd.param.cart.Cart;
import com.afd.service.order.ICartService;
import com.afd.service.product.IBrandShowService;
import com.afd.service.product.IProductService;
import com.afd.param.cart.CartItem;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.afd.constants.order.OrderConstants;
import com.afd.param.cart.CookieCartItem;
import com.afd.common.util.CartTransferUtils;

@Service("cartService")
public class CartServiceImpl implements ICartService{
	private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);
	
	@Autowired
	@Qualifier("redisNumber")
	private RedisTemplate<String, String> redisStock;
	@Autowired
	@Qualifier("redisTemplate")
	private RedisTemplate<String, String> redisCart;
	@Autowired
	private IProductService productService;
	@Autowired
	private IBrandShowService brandShowService;
	
	@Override
	public List<Cart> showCart(String cookieCart, boolean isLogin, long userId, int source, boolean isBuyNow) {
		// 合并购物车（cookie + server）
		List<CartItem> cartItems = this.combineCartItem(cookieCart, isLogin,
				userId, isBuyNow);
		
		List<Cart> carts = this.validCarts(cartItems, source, isLogin, userId);
		
		return carts;
	}
	
	List<CartItem> combineCartItem(String cookieCart, boolean isLogin, long userId, boolean isBuyNow) {
		List<CartItem> cartItems = null;
		if (!StringUtils.isBlank(cookieCart)) {
			// cookie中的购物车
			cartItems = CartTransferUtils.cookieCartToCartItem(JSON
					.parseObject(cookieCart,
							new TypeReference<List<CookieCartItem>>() {
							}));
		}
		Set<CartItem> svCartItem = new LinkedHashSet<CartItem>();
		
		if (cartItems != null && cartItems.size() > 0) {
			svCartItem.addAll(cartItems);
		}
		
		// 如果用户登录并且不是立即购买，再合并server端的购物车
		if (isLogin && !isBuyNow) {
			// 服务器端购物车
			String serverCart = "";
			try {
				serverCart = this.redisCart.opsForValue().get(
						OrderConstants.SERVER_CART + userId);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			List<CartItem> serverCartItems = CartTransferUtils
					.cookieCartToCartItem(JSON.parseObject(serverCart,
							new TypeReference<List<CookieCartItem>>() {
							}));
			
			// 合并服务器端购物车
			if (serverCartItems != null && serverCartItems.size() > 0) {
				for (CartItem serverCartItem : serverCartItems) {
					if (svCartItem.contains(serverCartItem)) {
						if (cartItems != null && cartItems.size() > 0) {
							CartItem ci = cartItems.get(cartItems
									.indexOf(serverCartItem));
							serverCartItem.setSelected(ci.isSelected());
						}
						svCartItem.remove(serverCartItem);
						svCartItem.add(serverCartItem);
					} else {
						svCartItem.add(serverCartItem);
					}
				}
			}
		}

		return new ArrayList<CartItem>(svCartItem);
	}

	List<Cart> validCarts(List<CartItem> cartItems, int source, boolean isLogin, long userId) {
		Map<Long,Cart> cartMap = new LinkedHashMap<Long,Cart>();
		if(null != cartItems && cartItems.size() > 0) {
			Map<Long,BrandShowDetail> bsDetailMap = getBSDetailMap(cartItems);
			Map<Long,BrandShow> brandShowMap = this.getBrandShowMap(bsDetailMap.values());
			
			for(CartItem cartItem : cartItems) {
				BrandShowDetail bsDetail = bsDetailMap.get(cartItem.getBrandShowDetailId());
				if(null == bsDetail) {
					continue;
				}
				this.validCartItem(cartItem, bsDetail);
				if(cartMap.containsKey(bsDetail.getBrandShowId())) {
					Cart cart = cartMap.get(bsDetail.getBrandShowId());
					cart.getCartItems().add(cartItem);
				} else {
					Cart cart = new Cart();
					BrandShow brandShow = brandShowMap.get(bsDetail.getBrandShowId());
					cart.setBrandShowId(brandShow.getBrandShowId());
					cart.setBrandShowTitle(brandShow.getTitle());
					cart.setSellerId(brandShow.getSellerId());
					cart.getCartItems().add(cartItem);
					cartMap.put(brandShow.getBrandShowId(), cart);
				}
			}
		}
		for (Cart cart : cartMap.values()) {
			List<CartItem> sortCartItems = cart.getCartItems();
			Collections.sort(sortCartItems, new Comparator<CartItem>() {
				public int compare(CartItem o1, CartItem o2) {
					return o1.getSortWeight() - o2.getSortWeight();
				}
			});
		}
		return  new ArrayList<Cart>(cartMap.values());
	}
	
	public List<CartItem> modifyQuantity(String cookieCart, long bsDetailId, long newQuantity, 
			long oldQuantity, boolean isLogin, long userId, int source, boolean isBuyNow) {
		List<CartItem> cartItems = this.combineCartItem(cookieCart, isLogin, userId, isBuyNow);
		for(CartItem cartItem : cartItems) {
			if(cartItem.getBrandShowDetailId() == bsDetailId) {
				BrandShowDetail bsDetail = this.brandShowService.getBrandShowDetailById(bsDetailId);
				this.validCartItem(cartItem, bsDetail);
				if(cartItem.getStatusCode() != 1) {
					cartItem.setNum(cartItem.getStock());//TODO
				}
			}
		}
		return cartItems;
	}
	
	public List<CartItem> deleteCartItems(String cookieCart,Set<Long> bsDetailIds, boolean isLogin, long userId) {
		List<CartItem> cartItems = this.combineCartItem(cookieCart, isLogin, userId, false);
		Iterator<CartItem> cartItemIterator = cartItems.iterator();
		while (cartItemIterator.hasNext()) {
			CartItem cartItem = cartItemIterator.next();
			if (bsDetailIds.contains(cartItem.getSkuId())) {
				cartItemIterator.remove();
				bsDetailIds.remove(cartItem.getSkuId());
				if (cartItems.size() == 0 || bsDetailIds == null
						|| bsDetailIds.size() == 0) {
					break;
				}
			}
		}
		return cartItems;
	}

	public List<CartItem> chgChecked(String cookieCart, Set<Long> bsDetailIds,
			boolean checked, boolean isLogin, long userId) {
		List<CartItem> cartItems = this.combineCartItem(cookieCart, isLogin, userId, false);
		if (cartItems != null && cartItems.size() > 0) {
			if (bsDetailIds != null && bsDetailIds.size() > 0) {
				for (CartItem cartItem : cartItems) {
					if (bsDetailIds.contains(cartItem.getBrandShowDetailId())) {
						cartItem.setSelected(checked);
					}
				}
			}
		}
		return cartItems;
	}
	
	public List<CartItem> clearFailureProduct(String cookieCart, boolean isLogin, long userId, int source) {
		List<CartItem> cartItems = this.combineCartItem(cookieCart, isLogin, userId, false);
		if(null != cartItems && cartItems.size() > 0) {
			Map<Long,BrandShowDetail> bsDetailMap = getBSDetailMap(cartItems);
			Iterator<CartItem> cartItemIt = cartItems.iterator();
			while (cartItemIt.hasNext()) {
				CartItem cartItem = cartItemIt.next();
				this.validCartItem(cartItem, bsDetailMap.get(cartItem.getBrandShowDetailId()));
				if(cartItem.getStatusCode() < 0) {//TODO
					cartItemIt.remove();
				}
			}
		}
		return cartItems;
	}
	
	public List<CartItem> filterSelectedCart(List<CartItem> cartItems) {
		if (cartItems != null && cartItems.size() > 0) {
			Iterator<CartItem> cartItemIterator = cartItems.iterator();
			while (cartItemIterator.hasNext()) {
				CartItem cartItem = cartItemIterator.next();
				if (!cartItem.isSelected()) {
					cartItemIterator.remove();
				}
			}
		}
		return cartItems;
	}
	
	public boolean hasErrorOnSelectedCart(List<CartItem> cartItems, int source,
			boolean isLogin, long userId) {
		this.validCarts(cartItems, source, isLogin, userId);
		return this.hasErrorOnCarts(cartItems);
	}

	private boolean hasErrorOnCarts(List<CartItem> cartItems) {
		if (cartItems != null && cartItems.size() > 0) {
			Iterator<CartItem> cartItemIterator = cartItems.iterator();
			while (cartItemIterator.hasNext()) {
				CartItem cartItem = cartItemIterator.next();
				if (cartItem.getStatusCode() != OrderConstants.CARTITEM_SUCCESS) {//TODO
					return true;
				}
			}
			return false;
		}
		return true;
	}
	
	private void validCartItem(CartItem cartItem, BrandShowDetail bsDetail) {
		if (bsDetail != null) {
			cartItem.setProdId(Integer.parseInt(bsDetail.getProdId().toString()));
			cartItem.setProdName(bsDetail.getProdTitle());
			cartItem.setBrandShowDetailId(bsDetail.getbSDId());
			cartItem.setProdImgUrl(bsDetail.getProdImg());
			cartItem.setSpecId(bsDetail.getSku().getSkuSpecId());
			cartItem.setSpecId(bsDetail.getSkuSpecName());
			cartItem.setSpecs(this.getSpec(bsDetail.getSkuSpecName()));
			cartItem.setMaketPrice(bsDetail.getSku().getMarketPrice());
			cartItem.setShowPrice(bsDetail.getShowPrice());
			cartItem.setStock((bsDetail.getShowBalance()-bsDetail.getSaleAmount() > 0) ? (bsDetail.getShowBalance()-bsDetail.getSaleAmount()) : 0l);
			cartItem.setBcId(bsDetail.getProduct().getBcId());
			cartItem.setWeight(bsDetail.getProduct().getWeight());
			cartItem.setVolume(new BigDecimal(StringUtils.isNotEmpty(bsDetail
					.getProduct().getVolume()) ? bsDetail.getProduct().getVolume()
					: "0"));
			cartItem.setProdCode(bsDetail.getProdCode());
			cartItem.setSkuCode(bsDetail.getSkuCode());

			long checkStatus = this.checkBsDetailStatusAndStock(bsDetail,
					cartItem.getNum());

			cartItem.setStatusCode(checkStatus);
//			// 购物车商品异常
//			if (cartItem.getStatusCode() != OrderConstants.CARTITEM_SUCCESS
//					&& cartItem.getStatusCode() != OrderConstants.CARTITEM_SKU_LOWSTOCK) {
//				cartItem.setSelected(false);
//				cartItem.setSortWeight(OrderConstants.CARTITEM_SORTWEIGHT_3);
//			}
//			// 购物车商品正常
//			else {
//				if (cartItem.getStatusCode() == OrderConstants.CARTITEM_SUCCESS) {
//					cartItem.setSortWeight(OrderConstants.CARTITEM_SORTWEIGHT_1);
//				}
//				if (cartItem.getStatusCode() == OrderConstants.CARTITEM_SKU_LOWSTOCK) {
//					cartItem.setSortWeight(OrderConstants.CARTITEM_SORTWEIGHT_2);
//				}
//			}
		}
	}
	
	/**
	 * 取得购物车中特卖明细Map
	 * @param cartItems
	 * @return
	 */
	private Map<Long,BrandShowDetail> getBSDetailMap(List<CartItem> cartItems) {
		// 取得所有特卖明细ID
		List<Long> bsdIds = this.getBSDIDsByCartItems(cartItems);
		List<BrandShowDetail> bsDetails = this.brandShowService.getBrandShowDetailsByIds(bsdIds);
		Set<Integer> skuIds = new LinkedHashSet<Integer>();
		Set<Integer> prodIds = new LinkedHashSet<Integer>();
		// 为skuIds和prodIds赋值
		this.setSkuAndProdIdsByBSDetails(bsDetails, skuIds, prodIds);
		List<Sku> skus = this.productService.getSkusBySkuIds(Arrays.asList(skuIds.toArray(new Integer[0])));
		List<Product> products = this.productService.getProductsByProdIds(Arrays.asList(prodIds.toArray(new Integer[0])));
		Map<Integer,Sku> skuMap = this.getSkuMap(skus);
		Map<Integer,Product> prodMap =  this.getProdMap(products);

		return this.getBSDetailMap(bsDetails,skuMap,prodMap);
	}
	
	private Map<Long,BrandShow> getBrandShowMap(Collection<BrandShowDetail> bsDetails) {
		// 取得所有特卖ID
		List<Long> brandShowIds = this.getBrandShowIds(bsDetails);
		List<BrandShow> brandShows = this.brandShowService.getBrandShowByIds(brandShowIds);
		// 取得特卖map
		return this.getBrandShowMap(brandShows);
	}
	
	/**
	 * Get购物车中All特卖明细ID
	 * @param cartItems
	 * @return
	 */
	private List<Long> getBSDIDsByCartItems(List<CartItem> cartItems) {
		List<Long> bsdIds = new ArrayList<Long>();
		if(null != cartItems && cartItems.size() > 0) {
			for(CartItem cartItem : cartItems) {
				bsdIds.add(cartItem.getBrandShowDetailId());
			}
		}
		return bsdIds;
	}
	
	/**
	 * 取得All特卖明细的skuId,prodId
	 * @param bsDetails
	 * @param skuIds
	 * @param prodIds
	 */
	private void setSkuAndProdIdsByBSDetails(List<BrandShowDetail> bsDetails, Set<Integer> skuIds, Set<Integer> prodIds) {
		if(null != bsDetails && bsDetails.size() > 0) {
			for(BrandShowDetail bsDetail : bsDetails) {
				skuIds.add(Integer.parseInt(bsDetail.getSkuId().toString()));
				prodIds.add(Integer.parseInt(bsDetail.getProdId().toString()));
			}
		}
	}
	
	/**
	 * 取得sku map
	 * @param skus
	 * @return
	 */
	private Map<Integer,Sku> getSkuMap(List<Sku> skus) {
		Map<Integer,Sku> skuMap = new LinkedHashMap<Integer,Sku>();
		if(null != skus && skus.size() > 0) {
			for(Sku sku : skus) {
				skuMap.put(sku.getSkuId(), sku);
			}
		}
		return skuMap;
	}
	
	/**
	 * 取得商品map
	 * @param prods
	 * @return
	 */
	private Map<Integer,Product> getProdMap(List<Product> prods) {
		Map<Integer,Product> prodMap = new LinkedHashMap<Integer,Product>();
		if(null != prods && prods.size() > 0) {
			for(Product prod : prods) {
				prodMap.put(prod.getProdId(), prod);
			}
		}
		return prodMap;
	}
	
	/**
	 * 取得特卖明细Map
	 * 		1.Set特卖明细的sku
	 * 		2.Set特卖明细的prod
	 * 		3.取得Map
	 * @param bsDetails
	 * @param skuMap
	 * @param prodMap
	 * @return
	 */
	private Map<Long,BrandShowDetail> getBSDetailMap(List<BrandShowDetail> bsDetails, Map<Integer,Sku> skuMap, Map<Integer,Product> prodMap) {
		Map<Long,BrandShowDetail> bsDetailMap = new LinkedHashMap<Long,BrandShowDetail>();
		if(null != bsDetails && bsDetails.size() > 0) {
			for(BrandShowDetail bsDetail : bsDetails) {
				bsDetail.setSku(skuMap.get(bsDetail.getSkuId()));
				bsDetail.setProduct(prodMap.get(bsDetail.getProdId()));
				bsDetailMap.put(bsDetail.getbSDId(), bsDetail);
			}
		}
		return bsDetailMap;
	}
	
	/**
	 * 取得All特卖ID
	 * @param bsDetails
	 * @return
	 */
	private List<Long> getBrandShowIds(Collection<BrandShowDetail> bsDetails) {
		Set<Long> brandShowIds = new LinkedHashSet<Long>();
		if(null != bsDetails && bsDetails.size() > 0) {
			for(BrandShowDetail bsDetail : bsDetails) {
				brandShowIds.add(bsDetail.getBrandShowId());
			}
		}
		return new ArrayList<Long>(brandShowIds);
	}
	
	private Map<Long,BrandShow> getBrandShowMap(List<BrandShow> brandShows) {
		Map<Long,BrandShow> brandShowMap = new LinkedHashMap<Long,BrandShow>();
		if(null != brandShows && brandShows.size() > 0) {
			for(BrandShow brandShow : brandShows) {
				brandShowMap.put(brandShow.getBrandShowId(), brandShow);
			}
		}
		return brandShowMap;
	}

	/**
	 * 获取规格与规格值
	 * 
	 * @param skuSpec
	 * @return
	 */
	private List<Map<String, String>> getSpec(String skuSpec) {
		List<Map<String, String>> rtn = new ArrayList<Map<String, String>>();
		if (!StringUtils.isBlank(skuSpec)) {
			String[] specs = skuSpec.split("\\|\\|\\|");
			if (specs != null && specs.length > 0) {
				for (String spec : specs) {
					Map<String, String> specTemp = new HashMap<String, String>();
					String[] opt = spec.split("\\:\\:\\:");
					if (opt != null && opt.length > 1) {
						String specName = opt[0];
						String specVal = opt[1];
						specTemp.put(specName, specVal);
					}
					rtn.add(specTemp);
				}
			}
		}

		return rtn;
	}
	
	private Long checkBsDetailStatusAndStock(BrandShowDetail bsDetail, Long num) {
		if(this.isBrandShowDetailNormal(bsDetail) != 1) {
			//TODO
			return 0l;
		}
		if(!this.isSkuNormal(bsDetail.getSku())) {
			//TODO
			return 0l;
		}
		if(!this.isProductNormal(bsDetail.getProduct())) {
			//TODO
			return 0l;
		}
		if(bsDetail.getShowBalance() - bsDetail.getSaleAmount() <= 0l) {
			//TODO
			return 0l;
		} else if(bsDetail.getShowBalance() - bsDetail.getSaleAmount() < num) {
			//TODO
			return 0l;
		} else if(bsDetail.getPurchaseCountLimit() < num) {
			//TODO
			return 0l;
		}
		return 0l;
	}
	
	/**
	 * 特卖明细状态
	 * @param bsDetail
	 * @return 正常/异常/过期
	 */
	private int isBrandShowDetailNormal(BrandShowDetail bsDetail) {
		//TODO
		return 1;
	}
	
	private boolean isSkuNormal(Sku sku) {
		//TODO
		return true;
	}
	
	private boolean isProductNormal(Product prod) {
		//TODO
		return true;
	}
}
