package com.afd.order.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import com.afd.constants.product.ProductConstants;
import com.afd.param.cart.CookieCartItem;
import com.afd.common.util.CartTransferUtils;
import com.afd.param.cart.MiniCart;

@Service("cartService")
public class CartServiceImpl implements ICartService{
	private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);
	
	@Autowired
	@Qualifier("redisNumber")
	private RedisTemplate<String, String> redisStock;
	@Autowired
	private IProductService productService;
	@Autowired
	private IBrandShowService brandShowService;
	
	// 迷你购物车返回条目限制
	private final int limitMiniCartCount = 5;

	@Override
	public List<Cart> showCart(String cookieCart) {
		List<CartItem> cartItems = this.getCartItemsByCookie(cookieCart);
		
		List<Cart> carts = this.validCarts(cartItems);
		
		return carts;
	}
	
	@Override
	public List<CartItem> getCartItemsByCookie(String cookieCart) {
		List<CartItem> cartItems = null;
		if (!StringUtils.isBlank(cookieCart)) {
			cartItems = CartTransferUtils.cookieCartToCartItem(JSON
					.parseObject(cookieCart,
							new TypeReference<List<CookieCartItem>>() {
							}));
		}

		return cartItems;
	}

	@Override
	public MiniCart showMiniCart(String cookieCart) {
		List<Cart> carts = this.showCart(cookieCart);
		long totalNum = 0;
		BigDecimal totalMoney = BigDecimal.ZERO;
		if (carts != null && carts.size() > 0) {
			int count = 0;
			Iterator<Cart> cartIterator = carts.iterator();
			while (cartIterator.hasNext()) {
				Cart cart = cartIterator.next();
				List<CartItem> cartItems = cart.getCartItems();
				if (cartItems != null && cartItems.size() > 0) {
					Iterator<CartItem> ciIterator = cartItems.iterator();
					while (ciIterator.hasNext()) {
						CartItem cartItem = ciIterator.next();
						if (cartItem.getStatusCode() == OrderConstants.CARTITEM_SUCCESS
								|| cartItem.getStatusCode() == OrderConstants.CARTITEM_BS_DETAIL_LOWSTOCK
								|| cartItem.getStatusCode() == OrderConstants.CARTITEM_BS_DETAIL_EXCEED) {
							totalMoney = totalMoney
									.add(cartItem.getShowPrice().multiply(
											new BigDecimal(cartItem.getNum())));
							totalNum++;
						} else {
							ciIterator.remove();
							continue;
						}
						if (count >= limitMiniCartCount) {
							ciIterator.remove();
						}
						count++;
					}
				}
				if (cartItems == null || cartItems.size() == 0) {
					cartIterator.remove();
				}
			}
		}
		MiniCart miniCart = new MiniCart();
		miniCart.setCarts(carts);
		miniCart.setTotalMoney(totalMoney);
		miniCart.setTotalNum(totalNum);
		return miniCart;

	}
	
	List<Cart> validCarts(List<CartItem> cartItems) {
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
				if(cartMap.containsKey(bsDetail.getBrandShowId().longValue())) {
					Cart cart = cartMap.get(bsDetail.getBrandShowId().longValue());
					cart.getCartItems().add(cartItem);
				} else {
					Cart cart = new Cart();
					BrandShow brandShow = brandShowMap.get(bsDetail.getBrandShowId().longValue());
					cart.setBrandShowId(brandShow.getBrandShowId().longValue());
					cart.setBrandShowTitle(brandShow.getTitle());
					cart.setSellerId(brandShow.getSellerId().longValue());
					cart.getCartItems().add(cartItem);
					cartMap.put(brandShow.getBrandShowId().longValue(), cart);
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
	
	public List<CartItem> modifyQuantity(String cookieCart, Long bsDetailId, long newQuantity, long oldQuantity) {
		List<CartItem> cartItems = this.getCartItemsByCookie(cookieCart);
		for(CartItem cartItem : cartItems) {
			if(cartItem.getBrandShowDetailId() == bsDetailId) {
				cartItem.setNum(newQuantity);
				BrandShowDetail bsDetail = this.brandShowService.getBrandShowDetailById(bsDetailId.intValue());
				bsDetail.setSku(this.productService.getSkuById(bsDetail.getSkuId().intValue()));
				bsDetail.setProduct(this.productService.getProductById(bsDetail.getProdId().intValue()));
				this.validCartItem(cartItem, bsDetail);
				if(cartItem.getStatusCode() == OrderConstants.CARTITEM_BS_DETAIL_LOWSTOCK
						|| cartItem.getStatusCode() == OrderConstants.CARTITEM_BS_DETAIL_EXCEED) {
					if(cartItem.getStock() > 0l && cartItem.getPurchaseCountLimit() > 0) {
						cartItem.setNum(Math.min(cartItem.getStock(), cartItem.getPurchaseCountLimit()));
					} else if (cartItem.getStock() > 0l) {
						cartItem.setNum(cartItem.getStock());
					}
				}
			}
		}
		return cartItems;
	}
	
	public List<CartItem> deleteCartItems(String cookieCart,Set<Long> bsDetailIds) {
		List<CartItem> cartItems = this.getCartItemsByCookie(cookieCart);
		Iterator<CartItem> cartItemIterator = cartItems.iterator();
		while (cartItemIterator.hasNext()) {
			CartItem cartItem = cartItemIterator.next();
			if (bsDetailIds.contains(cartItem.getBrandShowDetailId())) {
				cartItemIterator.remove();
				bsDetailIds.remove(cartItem.getBrandShowDetailId());
				if (cartItems.size() == 0 || bsDetailIds == null
						|| bsDetailIds.size() == 0) {
					break;
				}
			}
		}
		return cartItems;
	}

	public List<CartItem> chgChecked(String cookieCart, Set<Long> bsDetailIds, boolean checked) {
		List<CartItem> cartItems = this.getCartItemsByCookie(cookieCart);
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
	
	public List<CartItem> clearFailureProduct(String cookieCart) {
		List<CartItem> cartItems = this.getCartItemsByCookie(cookieCart);
		if(null != cartItems && cartItems.size() > 0) {
			Map<Long,BrandShowDetail> bsDetailMap = getBSDetailMap(cartItems);
			Iterator<CartItem> cartItemIt = cartItems.iterator();
			while (cartItemIt.hasNext()) {
				CartItem cartItem = cartItemIt.next();
				this.validCartItem(cartItem, bsDetailMap.get(cartItem.getBrandShowDetailId()));
				if(cartItem.getStatusCode() != OrderConstants.CARTITEM_SUCCESS
						&& cartItem.getStatusCode() != OrderConstants.CARTITEM_BS_DETAIL_LOWSTOCK
						&& cartItem.getStatusCode() != OrderConstants.CARTITEM_BS_DETAIL_EXCEED) {
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
	
	public boolean hasErrorOnSelectedCart(List<CartItem> cartItems) {
		this.validCarts(cartItems);
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
			cartItem.setProdId(bsDetail.getProdId().intValue());
			cartItem.setProdName(bsDetail.getProdTitle());
			cartItem.setProdCode(bsDetail.getProdCode());
			cartItem.setSkuId(bsDetail.getSkuId().intValue());
			cartItem.setSkuCode(bsDetail.getSkuCode());
			cartItem.setBrandShowDetailId(bsDetail.getbSDId().longValue());
			cartItem.setProdImgUrl(bsDetail.getProdImg());
			if(null != bsDetail.getSku()){
				cartItem.setSpecId(bsDetail.getSku().getSkuSpecId());
				cartItem.setMaketPrice(bsDetail.getSku().getMarketPrice());
			}
			cartItem.setSpecName(bsDetail.getSkuSpecName());
			cartItem.setSpecs(this.getSpec(bsDetail.getSkuSpecName()));
			cartItem.setShowPrice(bsDetail.getShowPrice());
			if(null != bsDetail.getProduct()) {
				cartItem.setBcId(bsDetail.getProduct().getBcId());
			}
			Long stock = bsDetail.getShowBalance().longValue()-bsDetail.getSaleAmount().longValue();
			cartItem.setStock(stock > 0 ? stock : 0l);
			cartItem.setPurchaseCountLimit((bsDetail.getPurchaseCountLimit()!=null && bsDetail.getPurchaseCountLimit() > 0) 
					? bsDetail.getPurchaseCountLimit() : 0);
			long checkStatus = this.checkBsDetailStatusAndStock(bsDetail, cartItem.getStock(), cartItem.getNum());

			cartItem.setStatusCode(checkStatus);
			// 购物车商品异常
			if (cartItem.getStatusCode() != OrderConstants.CARTITEM_SUCCESS
					&& cartItem.getStatusCode() != OrderConstants.CARTITEM_BS_DETAIL_LOWSTOCK
					&& cartItem.getStatusCode() != OrderConstants.CARTITEM_BS_DETAIL_EXCEED) {
				cartItem.setSelected(false);
				cartItem.setSortWeight(OrderConstants.CARTITEM_SORTWEIGHT_3);
			}
			// 购物车商品正常
			else {
				if (cartItem.getStatusCode() == OrderConstants.CARTITEM_SUCCESS) {
					cartItem.setSortWeight(OrderConstants.CARTITEM_SORTWEIGHT_1);
				}
				if (cartItem.getStatusCode() == OrderConstants.CARTITEM_BS_DETAIL_LOWSTOCK
						|| cartItem.getStatusCode() == OrderConstants.CARTITEM_BS_DETAIL_EXCEED) {
					cartItem.setSortWeight(OrderConstants.CARTITEM_SORTWEIGHT_2);
				}
			}
		}
	}
	
	/**
	 * 取得购物车中特卖明细Map
	 * @param cartItems
	 * @return
	 */
	private Map<Long,BrandShowDetail> getBSDetailMap(List<CartItem> cartItems) {
		// 取得所有特卖明细ID
		List<Integer> bsdIds = this.getBSDIDsByCartItems(cartItems);
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
		List<Integer> brandShowIds = this.getBrandShowIds(bsDetails);
		List<BrandShow> brandShows = this.brandShowService.getBrandShowByIds(brandShowIds);
		// 取得特卖map
		Map<Long,BrandShow> brandShowMap = new LinkedHashMap<Long,BrandShow>();
		if(null != brandShows && brandShows.size() > 0) {
			for(BrandShow brandShow : brandShows) {
				brandShowMap.put(brandShow.getBrandShowId().longValue(), brandShow);
			}
		}
		return brandShowMap;
	}
	
	/**
	 * Get购物车中All特卖明细ID
	 * @param cartItems
	 * @return
	 */
	private List<Integer> getBSDIDsByCartItems(List<CartItem> cartItems) {
		List<Integer> bsdIds = new ArrayList<Integer>();
		if(null != cartItems && cartItems.size() > 0) {
			for(CartItem cartItem : cartItems) {
				bsdIds.add(cartItem.getBrandShowDetailId().intValue());
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
				skuIds.add(bsDetail.getSkuId().intValue());
				prodIds.add(bsDetail.getProdId().intValue());
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
				bsDetail.setSku(skuMap.get(bsDetail.getSkuId().intValue()));
				bsDetail.setProduct(prodMap.get(bsDetail.getProdId().intValue()));
				bsDetailMap.put(bsDetail.getbSDId().longValue(), bsDetail);
			}
		}
		return bsDetailMap;
	}
	
	/**
	 * 取得All特卖ID
	 * @param bsDetails
	 * @return
	 */
	private List<Integer> getBrandShowIds(Collection<BrandShowDetail> bsDetails) {
		Set<Integer> brandShowIds = new LinkedHashSet<Integer>();
		if(null != bsDetails && bsDetails.size() > 0) {
			for(BrandShowDetail bsDetail : bsDetails) {
				brandShowIds.add(bsDetail.getBrandShowId());
			}
		}
		return new ArrayList<Integer>(brandShowIds);
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
	
	private Long checkBsDetailStatusAndStock(BrandShowDetail bsDetail, Long stock, Long num) {
		if(null == bsDetail) {
			return OrderConstants.CARTITEM_BS_DETAIL_IS_NULL;
		}
		int bsDetailStatus = this.isBrandShowDetailNormal(bsDetail);
		if(bsDetailStatus != 1) {
			if(bsDetailStatus == -1) {
				return OrderConstants.CARTITEM_BS_DETAIL_STATUS_UNNORMAL;
			} else if (bsDetailStatus == -2) {
				return OrderConstants.CARTITEM_BS_DETAIL_EXPIRED;
			}
			return OrderConstants.CARTITEM_BS_DETAIL_STATUS_UNNORMAL;
		}
		if(!this.isSkuNormal(bsDetail.getSku())) {
			return OrderConstants.CARTITEM_SKU_STATUS_UNNORMAL;
		}
		if(!this.isProductNormal(bsDetail.getProduct())) {
			return OrderConstants.CARTITEM_PROD_STATUS_UNNORMAL;
		}
		if(stock <= 0l) {
			return OrderConstants.CARTITEM_BS_DETAIL_OUTOFSTOCK;
		} else if(stock < num) {
			return OrderConstants.CARTITEM_BS_DETAIL_LOWSTOCK;
		} else if(null != bsDetail.getPurchaseCountLimit() && bsDetail.getPurchaseCountLimit() > 0 && bsDetail.getPurchaseCountLimit() < num) {
			return OrderConstants.CARTITEM_BS_DETAIL_EXCEED;
		}
		return 0l;
	}
	
	/**
	 * 特卖明细状态
	 * @param bsDetail
	 * @return 1:正常; -1:异常; -2:过期
	 */
	private int isBrandShowDetailNormal(BrandShowDetail bsDetail) {
		Date now = new Date();
		Date endDate = bsDetail.getEndDate();
		if(!"1".equals(bsDetail.getStatus())) {//TODO 状态
			return -1;
		}
		BrandShow brandShow = this.brandShowService.getBrandShowById(bsDetail.getBrandShowId());
		if(brandShow.getStatus() != ProductConstants.BrandShow$Status.ONLINE){
			if(now.after(endDate)) {
				return -2;
			}
		}
		return 1;
	}
	
	private boolean isSkuNormal(Sku sku) {
		//TODO 状态
		if(null == sku) {
			return false;
		}
		return true;
	}
	
	private boolean isProductNormal(Product prod) {
		if(null == prod) { //TODO 状态
			return false;
		}
		return true;
	}
}
