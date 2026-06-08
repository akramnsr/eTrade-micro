package ma.portnet.authservice.client;

import ma.portnet.authservice.dto.response.ApiResponse;
import ma.portnet.authservice.dto.response.DemandSummaryDto;
import ma.portnet.authservice.dto.response.DemandDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "demand-service", configuration = FeignConfig.class)
public interface DemandServiceClient {

    @GetMapping("/api/v1/demands")
    ApiResponse<List<DemandSummaryDto>> listDemands(
            @RequestParam(required = false) String status
    );

    @GetMapping("/api/v1/demands/{id}")
    ApiResponse<DemandDetailDto> getDemand(@PathVariable("id") String id);

    @DeleteMapping("/api/v1/demands/{id}")
    ApiResponse<Void> deleteDemand(@PathVariable("id") String id);

    @PostMapping("/api/v1/demands/{id}/force-status")
    ApiResponse<DemandDetailDto> forceStatus(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> body
    );
}