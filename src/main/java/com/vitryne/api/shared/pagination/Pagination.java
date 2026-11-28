package com.vitryne.api.shared.pagination;

import com.vitryne.api.shared.exception.InvalidInputException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Regras de paginação comuns às listagens públicas do app. */
public final class Pagination {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private Pagination() {
    }

    /** Valida page e size vindos da requisição; valores inválidos viram 400. */
    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0) {
            throw new InvalidInputException("A página deve ser maior ou igual a 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidInputException(
                    "O tamanho da página deve estar entre 1 e " + MAX_SIZE
            );
        }
        return PageRequest.of(page, size, sort);
    }
}
