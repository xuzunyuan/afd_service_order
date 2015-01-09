package com.afd.order.dao;

import com.afd.model.order.YWProdPromotionDetail;

public interface YWProdPromotionDetailMapper {
    int deleteByPrimaryKey(Long orderItemId);

    int insert(YWProdPromotionDetail record);

    int insertSelective(YWProdPromotionDetail record);

    YWProdPromotionDetail selectByPrimaryKey(Long orderItemId);

    int updateByPrimaryKeySelective(YWProdPromotionDetail record);

    int updateByPrimaryKey(YWProdPromotionDetail record);
}