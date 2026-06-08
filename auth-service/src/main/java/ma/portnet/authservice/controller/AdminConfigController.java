package ma.portnet.authservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.portnet.authservice.dto.response.ApiResponse;
import ma.portnet.authservice.entity.SystemConfig;
import ma.portnet.authservice.repository.SystemConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/config")
@Tag(name = "Admin Config", description = "Configuration système")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminConfigController {

    private final SystemConfigRepository repo;

    public AdminConfigController(SystemConfigRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        SystemConfig c = repo.findById("default")
                .orElseGet(() -> repo.save(new SystemConfig()));
        return ResponseEntity.ok(ApiResponse.ok(toMap(c)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @RequestBody Map<String, Object> body
    ) {
        SystemConfig c = repo.findById("default").orElseGet(SystemConfig::new);

        if (body.containsKey("defaultRateMAD"))       c.setDefaultRateMAD(bd(body.get("defaultRateMAD")));
        if (body.containsKey("defaultRateEUR"))       c.setDefaultRateEUR(bd(body.get("defaultRateEUR")));
        if (body.containsKey("defaultRateUSD"))       c.setDefaultRateUSD(bd(body.get("defaultRateUSD")));
        if (body.containsKey("commissionRate"))       c.setCommissionRate(bd(body.get("commissionRate")));
        if (body.containsKey("fixedFees"))            c.setFixedFees(bd(body.get("fixedFees")));
        if (body.containsKey("minOperationAmount"))   c.setMinOperationAmount(bd(body.get("minOperationAmount")));
        if (body.containsKey("maxOperationAmount"))   c.setMaxOperationAmount(bd(body.get("maxOperationAmount")));
        if (body.containsKey("analysisDeadlineDays")) c.setAnalysisDeadlineDays(((Number) body.get("analysisDeadlineDays")).intValue());
        if (body.containsKey("currenciesEnabled")) {
            Object v = body.get("currenciesEnabled");
            if (v instanceof List<?> list) {
                c.setCurrenciesEnabled(String.join(",", list.stream().map(Object::toString).toList()));
            } else if (v != null) {
                c.setCurrenciesEnabled(v.toString());
            }
        }
        c.setUpdatedAt(LocalDateTime.now());
        repo.save(c);
        return ResponseEntity.ok(ApiResponse.ok("Configuration mise à jour", toMap(c)));
    }

    private BigDecimal bd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        return new BigDecimal(v.toString());
    }

    private Map<String, Object> toMap(SystemConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("defaultRateMAD",       c.getDefaultRateMAD());
        m.put("defaultRateEUR",       c.getDefaultRateEUR());
        m.put("defaultRateUSD",       c.getDefaultRateUSD());
        m.put("commissionRate",       c.getCommissionRate());
        m.put("fixedFees",            c.getFixedFees());
        m.put("minOperationAmount",   c.getMinOperationAmount());
        m.put("maxOperationAmount",   c.getMaxOperationAmount());
        m.put("analysisDeadlineDays", c.getAnalysisDeadlineDays());
        m.put("currenciesEnabled",    c.getCurrenciesEnabled().split(","));
        m.put("updatedAt",            c.getUpdatedAt());
        return m;
    }
}