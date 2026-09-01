package com.example.mallu.product.service;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.result.PageResult;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.product.dto.ProductStockVO;
import com.example.mallu.product.dto.ProductVO;
import com.example.mallu.product.entity.Product;
import com.example.mallu.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private static final Integer DEFAULT_PAGE = 1;
    private static final Integer DEFAULT_SIZE = 10;

    public PageResult<ProductVO> listProducts(Integer page, Integer size) {
        page = page == null || page < 1 ? DEFAULT_PAGE : page;
        size = size == null || size < 1 ? DEFAULT_SIZE : size;

        Integer offset = (page - 1) * size;
        List<Product> products = productMapper.selectPage(offset, size);
        Long total = productMapper.countAll();

        List<ProductVO> vos = products.stream()
                .map(this::toProductVO)
                .toList();
        return PageResult.of(vos, total, page, size);
    }

    public ProductVO getProductDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        }
        return toProductVO(product);
    }

    public ProductStockVO getStock(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ProductStockVO vo = new ProductStockVO();
        vo.setProductId(id);
        vo.setStock(product.getStock());
        return vo;
    }

    private ProductVO toProductVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }
}
