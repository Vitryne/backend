package com.vitryne.api.exception;

public class UnavailableQuantityException extends RuntimeException {
    public UnavailableQuantityException(String size, Integer available, Integer requested) {
        super(String.format(
                "Requested quantity (%d) exceeds available stock (%d) for size %s",
                requested, available, size));
    }
}
