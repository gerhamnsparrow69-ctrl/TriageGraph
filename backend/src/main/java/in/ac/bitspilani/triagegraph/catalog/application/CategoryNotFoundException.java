package in.ac.bitspilani.triagegraph.catalog.application;

import in.ac.bitspilani.triagegraph.shared.exception.ResourceNotFoundException;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "CATEGORY_NOT_FOUND";

    public CategoryNotFoundException(String code) {
        super(CODE, "No category with code " + code);
    }
}
