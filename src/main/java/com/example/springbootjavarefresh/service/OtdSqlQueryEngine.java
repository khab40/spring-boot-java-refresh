package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.MarketData;
import com.example.springbootjavarefresh.entity.MarketDataType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OtdSqlQueryEngine {

    private static final Pattern QUERY_PATTERN = Pattern.compile(
            "(?is)^select\\s+(?<select>.+?)\\s+from\\s+market_data(?:\\s+where\\s+(?<where>.*?))?(?:\\s+order\\s+by\\s+(?<order>.*?))?(?:\\s+limit\\s+(?<limit>\\d+))?\\s*;?$"
    );

    public List<MarketData> execute(String sql, List<MarketData> sourceRows) {
        Matcher matcher = QUERY_PATTERN.matcher(sql == null ? "" : sql.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Only SELECT ... FROM market_data [WHERE ...] [ORDER BY ...] [LIMIT ...] is supported.");
        }

        String selectPart = matcher.group("select").trim();
        if (!"*".equals(selectPart)) {
            throw new IllegalArgumentException("The OTD SQL engine currently supports SELECT * only.");
        }

        Predicate<MarketData> predicate = parseWhereClause(matcher.group("where"));
        Comparator<MarketData> comparator = parseOrderBy(matcher.group("order"));
        int limit = matcher.group("limit") == null ? Integer.MAX_VALUE : Integer.parseInt(matcher.group("limit"));

        return sourceRows.stream()
                .filter(predicate)
                .sorted(comparator)
                .limit(limit)
                .toList();
    }

    private Predicate<MarketData> parseWhereClause(String whereClause) {
        if (whereClause == null || whereClause.isBlank()) {
            return row -> true;
        }

        String[] conditions = whereClause.trim().split("(?i)\\s+and\\s+");
        List<Predicate<MarketData>> predicates = new ArrayList<>();
        for (String condition : conditions) {
            predicates.add(parseCondition(condition.trim()));
        }

        return row -> predicates.stream().allMatch(predicate -> predicate.test(row));
    }

    private Predicate<MarketData> parseCondition(String condition) {
        Matcher inMatcher = Pattern.compile("(?i)^symbol\\s+in\\s*\\((.+)\\)$").matcher(condition);
        if (inMatcher.matches()) {
            String[] rawValues = inMatcher.group(1).split(",");
            List<String> symbols = new ArrayList<>();
            for (String rawValue : rawValues) {
                symbols.add(unquote(rawValue.trim()).toUpperCase(Locale.ROOT));
            }
            return row -> symbols.contains(row.getSymbol().toUpperCase(Locale.ROOT));
        }

        Matcher comparisonMatcher = Pattern.compile("(?i)^(symbol|data_type|market_date|timestamp|price|volume)\\s*(=|>=|<=|>|<)\\s*(.+)$")
                .matcher(condition);
        if (!comparisonMatcher.matches()) {
            throw new IllegalArgumentException("Unsupported WHERE clause condition: " + condition);
        }

        String field = comparisonMatcher.group(1).toLowerCase(Locale.ROOT);
        String operator = comparisonMatcher.group(2);
        String value = comparisonMatcher.group(3).trim();

        return switch (field) {
            case "symbol" -> compareString(row -> row.getSymbol(), operator, unquote(value));
            case "data_type" -> compareString(row -> row.getDataType().name(), operator, unquote(value));
            case "market_date" -> compareLocalDate(operator, LocalDate.parse(unquote(value)));
            case "timestamp" -> compareDateTime(operator, LocalDateTime.parse(unquote(value)));
            case "price" -> compareBigDecimal(operator, new BigDecimal(unquote(value)));
            case "volume" -> compareLong(operator, Long.parseLong(unquote(value)));
            default -> throw new IllegalArgumentException("Unsupported WHERE field: " + field);
        };
    }

    private Predicate<MarketData> compareString(java.util.function.Function<MarketData, String> extractor, String operator, String expected) {
        if (!"=".equals(operator)) {
            throw new IllegalArgumentException("Only equality is supported for string fields.");
        }
        return row -> extractor.apply(row).equalsIgnoreCase(expected);
    }

    private Predicate<MarketData> compareLocalDate(String operator, LocalDate expected) {
        return row -> compareComparable(row.getMarketDate(), operator, expected);
    }

    private Predicate<MarketData> compareDateTime(String operator, LocalDateTime expected) {
        return row -> compareComparable(row.getTimestamp(), operator, expected);
    }

    private Predicate<MarketData> compareBigDecimal(String operator, BigDecimal expected) {
        return row -> compareComparable(row.getPrice(), operator, expected);
    }

    private Predicate<MarketData> compareLong(String operator, long expected) {
        return row -> compareComparable(row.getVolume() == null ? 0L : row.getVolume(), operator, expected);
    }

    private <T extends Comparable<T>> boolean compareComparable(T actual, String operator, T expected) {
        int comparison = actual.compareTo(expected);
        return switch (operator) {
            case "=" -> comparison == 0;
            case ">=" -> comparison >= 0;
            case "<=" -> comparison <= 0;
            case ">" -> comparison > 0;
            case "<" -> comparison < 0;
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    private Comparator<MarketData> parseOrderBy(String orderClause) {
        Comparator<MarketData> defaultComparator = Comparator.comparing(MarketData::getTimestamp).reversed();
        if (orderClause == null || orderClause.isBlank()) {
            return defaultComparator;
        }

        Matcher matcher = Pattern.compile("(?i)^(timestamp|market_date|symbol|price|volume)(?:\\s+(asc|desc))?$").matcher(orderClause.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported ORDER BY clause: " + orderClause);
        }

        Comparator<MarketData> comparator = switch (matcher.group(1).toLowerCase(Locale.ROOT)) {
            case "timestamp" -> Comparator.comparing(MarketData::getTimestamp);
            case "market_date" -> Comparator.comparing(MarketData::getMarketDate);
            case "symbol" -> Comparator.comparing(MarketData::getSymbol, String.CASE_INSENSITIVE_ORDER);
            case "price" -> Comparator.comparing(MarketData::getPrice);
            case "volume" -> Comparator.comparing(row -> row.getVolume() == null ? 0L : row.getVolume());
            default -> defaultComparator;
        };

        return "desc".equalsIgnoreCase(matcher.group(2)) ? comparator.reversed() : comparator;
    }

    private String unquote(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
