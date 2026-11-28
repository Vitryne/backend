package com.vitryne.api.store.service;

import com.vitryne.api.shared.dto.PageResponseDTO;
import com.vitryne.api.shared.exception.InvalidInputException;
import com.vitryne.api.store.dto.OpeningPeriodDTO;
import com.vitryne.api.store.dto.StoreCatalogItemDTO;
import com.vitryne.api.store.entity.Store;
import com.vitryne.api.store.entity.StoreOpeningPeriod;
import com.vitryne.api.store.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;

@Service
public class StoreCatalogService {

    public static final int MAX_PAGE_SIZE = 50;

    private static final Sort BY_NAME = Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));

    private final StoreRepository storeRepository;
    private final Clock clock;

    @Autowired
    public StoreCatalogService(
            StoreRepository storeRepository,
            @Value("${vitryne.stores.timezone:America/Sao_Paulo}") String timezone
    ) {
        this(storeRepository, Clock.system(ZoneId.of(timezone)));
    }

    /** Relógio no fuso das lojas; os horários de funcionamento são locais. */
    StoreCatalogService(StoreRepository storeRepository, Clock clock) {
        this.storeRepository = storeRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<StoreCatalogItemDTO> listActiveStores(int page, int size) {
        if (page < 0) {
            throw new InvalidInputException("A página deve ser maior ou igual a 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidInputException(
                    "O tamanho da página deve estar entre 1 e " + MAX_PAGE_SIZE
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);

        return PageResponseDTO.from(
                storeRepository.findActiveWithActiveProducts(PageRequest.of(page, size, BY_NAME)),
                store -> toCatalogItem(store, now)
        );
    }

    private static StoreCatalogItemDTO toCatalogItem(Store store, LocalDateTime now) {
        return new StoreCatalogItemDTO(
                store.getId(),
                store.getName(),
                store.getDescription(),
                store.getMainCategory(),
                store.getDeliveryRadiusKm(),
                store.getPreparationTimeMinutes(),
                store.getPhotoUrl(),
                store.getBannerUrl(),
                store.getTags().stream().toList(),
                store.getOpeningPeriods().stream()
                        .sorted(Comparator.comparing(StoreOpeningPeriod::getDayOfWeek)
                                .thenComparing(StoreOpeningPeriod::getOpensAt))
                        .map(OpeningPeriodDTO::from)
                        .toList(),
                store.isOpenAt(now)
        );
    }
}
