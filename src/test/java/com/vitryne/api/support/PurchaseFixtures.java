package com.vitryne.api.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.atomic.AtomicLong;

/** Dados de compra criados direto no banco, com identificadores únicos por chamada. */
public final class PurchaseFixtures {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000);

    private final JdbcTemplate jdbc;

    public PurchaseFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Shop(long ownerId, long storeId) {
    }

    public Shop shop() {
        long n = SEQ.incrementAndGet();
        long owner = user("lojista" + n + "@teste.com", "STORE");
        long store = jdbc.queryForObject(
                "INSERT INTO store (owner_id, name) VALUES (?, ?) RETURNING id", Long.class, owner, "Loja " + n
        );
        return new Shop(owner, store);
    }

    public long product(Shop shop) {
        long n = SEQ.incrementAndGet();
        return jdbc.queryForObject("""
                INSERT INTO product (name, price, type, status, store_id, photo_urls, updated_at)
                VALUES (?, 19.90, 'CAMISETA', 'ACTIVE', ?, ARRAY['https://img/1.png'], now()) RETURNING id
                """, Long.class, "Camiseta " + n, shop.storeId());
    }

    public long stock(long productId, String size, int quantity) {
        return jdbc.queryForObject("""
                INSERT INTO stock (product_id, size, quantity, created_at, updated_at)
                VALUES (?, ?, ?, now(), now()) RETURNING id
                """, Long.class, productId, size, quantity);
    }

    /** Estoque de quantidade informada num produto novo de uma loja nova. */
    public long stock(int quantity) {
        return stock(product(shop()), "M", quantity);
    }

    /** Cliente com endereço de entrega selecionado e sem carrinho. */
    public long consumer() {
        long n = SEQ.incrementAndGet();
        long user = user("cliente" + n + "@teste.com", "CONSUMER");
        long client = jdbc.queryForObject(
                "INSERT INTO client (user_id, name, cpf) VALUES (?, 'Cliente', ?) RETURNING id",
                Long.class, user, String.format("%011d", n)
        );
        long address = jdbc.queryForObject("""
                INSERT INTO client_address (client_id, zip_code, street, address_number, neighborhood, city, state)
                VALUES (?, '87000000', 'Rua A', '10', 'Centro', 'Maringá', 'PR') RETURNING id
                """, Long.class, client);
        jdbc.update("UPDATE client SET selected_address_id = ? WHERE id = ?", address, client);
        return user;
    }

    /** Coloca o item no carrinho do cliente (criando o carrinho) e devolve o id do item. */
    public long cartItem(long userId, long stockId, int quantity) {
        Long cart = jdbc.query(
                "SELECT id FROM cart WHERE user_id = ?", rs -> rs.next() ? rs.getLong(1) : null, userId
        );
        if (cart == null) {
            cart = jdbc.queryForObject(
                    "INSERT INTO cart (user_id, total_value_forecast) VALUES (?, 0) RETURNING id", Long.class, userId
            );
        }
        long item = jdbc.queryForObject(
                "INSERT INTO cart_item (cart_id, stock_id, quantity, unit_price) VALUES (?, ?, ?, 19.90) RETURNING id",
                Long.class, cart, stockId, quantity
        );
        jdbc.update("UPDATE cart SET total_value_forecast = 19.90 * ? WHERE id = ?", quantity, cart);
        return item;
    }

    /** Cliente com carrinho contendo o item informado. */
    public long consumerWithCart(long stockId, int quantity) {
        long user = consumer();
        cartItem(user, stockId, quantity);
        return user;
    }

    public long user(String email, String role) {
        return jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, role) VALUES (?, 'x', ?) RETURNING id",
                Long.class, email, role
        );
    }

    public int stockQuantity(long stockId) {
        return jdbc.queryForObject("SELECT quantity FROM stock WHERE id = ?", Integer.class, stockId);
    }

    public int cartItemQuantity(long itemId) {
        return jdbc.queryForObject("SELECT quantity FROM cart_item WHERE id = ?", Integer.class, itemId);
    }

    public int cartItems(long userId) {
        return count("SELECT count(*) FROM cart_item ci JOIN cart c ON c.id = ci.cart_id WHERE c.user_id = ?", userId);
    }

    public int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }
}
