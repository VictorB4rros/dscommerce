package com.devsuperior.dscommerce.tests;

import com.devsuperior.dscommerce.dto.ProductDTO;
import com.devsuperior.dscommerce.entities.Category;
import com.devsuperior.dscommerce.entities.Product;

public class ProductFactory {

    public static Product createProduct() {
        Category category = CategoryFactory.createCategory();
        Product product = new Product(1L, "Console Playstation 5", "consectetur adipiscing elit, sed", 3999.0, "https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/4-big.jpg");
        product.getCategories().add(category);
        return product;
    }

    public static Product createProduct(String productName) {
        Product product = createProduct();
        product.setName(productName);
        return product;
    }

    public static Product createProductToInsert() {
        Category category = CategoryFactory.createCategory();
        Product product = new Product(null, "Console Playstation 5", "consectetur adipiscing elit, sed", 3999.0, "https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/4-big.jpg");
        product.getCategories().add(category);
        return product;
    }

    public static Product createProductWithNullName() {
        Product product = createProductToInsert();
        product.setName(null);
        return product;
    }

    public static Product createProductWithNullDescription() {
        Product product = createProductToInsert();
        product.setDescription(null);
        return product;
    }

    public static Product createProductWithNegativePrice() {
        Product product = createProductToInsert();
        product.setPrice(-1.0);
        return product;
    }

    public static Product createProductWithZeroPrice() {
        Product product = createProductToInsert();
        product.setPrice(0.0);
        return product;
    }


    public static Product createProductWithNoCategories() {
        Product product = createProductToInsert();
        product.getCategories().clear();
        return product;
    }

    public static ProductDTO createProductDTO() {
        Product product = createProductToInsert();
        return new ProductDTO(product);
    }

    public static ProductDTO createInvalidProductDTO(String field) {
        switch (field) {
            case "name" -> {
                Product product = createProductWithNullName();
                return new ProductDTO(product);
            }
            case "description" -> {
                Product product = createProductWithNullDescription();
                return new ProductDTO(product);
            }
            case "negative price" -> {
                Product product = createProductWithNegativePrice();
                return new ProductDTO(product);
            }
            case "price equals zero" -> {
                Product product = createProductWithZeroPrice();
                return new ProductDTO(product);
            }
            default -> {
                Product product = createProductWithNoCategories();
                return new ProductDTO(product);
            }
        }
    }
}
