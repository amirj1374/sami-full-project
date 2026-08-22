package com.sami.app.siminvestment;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sim-investment")
@RequiredArgsConstructor
public class SimInvestmentController {
    private final SimInvestmentQueryService queries;
    private final SimInvestmentImportService imports;
    private final SimInvestmentAnalysisService analysis;

    @GetMapping("/overview") @PreAuthorize("@authz.has('sim-investment:view')")
    public ApiResponse<Map<String,Object>> overview(){return ApiResponse.ok(queries.overview());}

    @GetMapping("/numbers") @PreAuthorize("@authz.has('sim-investment:view')")
    public ApiResponse<PageResponse<Map<String,Object>>> numbers(@RequestParam(required=false)String search,@RequestParam(required=false)String numberClass,
            @RequestParam(required=false)String confidence,@RequestParam(required=false)String liquidity,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){
        return ApiResponse.ok(queries.numbers(search,numberClass,confidence,liquidity,page,size));
    }

    @GetMapping("/numbers/{phone}") @PreAuthorize("@authz.has('sim-investment:view')")
    public ApiResponse<Map<String,Object>> detail(@PathVariable String phone){return ApiResponse.ok(queries.detail(phone));}

    @GetMapping("/opportunities") @PreAuthorize("@authz.has('sim-investment:view')")
    public ApiResponse<List<Map<String,Object>>> opportunities(@RequestParam(defaultValue="20")int limit){return ApiResponse.ok(queries.opportunities(limit));}

    @GetMapping("/imports") @PreAuthorize("@authz.has('sim-investment:view-history')")
    public ApiResponse<List<Map<String,Object>>> importHistory(){return ApiResponse.ok(imports.imports());}

    @GetMapping("/imports/{id}/messages") @PreAuthorize("@authz.has('sim-investment:view-history')")
    public ApiResponse<List<Map<String,Object>>> importMessages(@PathVariable long id){return ApiResponse.ok(imports.messages(id));}

    @PostMapping(value="/imports",consumes="multipart/form-data") @PreAuthorize("@authz.has('sim-investment:import')")
    public ApiResponse<Map<String,Object>> importFile(@RequestParam("file")MultipartFile file,@RequestParam(required=false)String sourceCode,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate observedOn,@RequestParam(defaultValue="true")boolean fullSnapshot){
        return ApiResponse.ok(imports.importFile(file,sourceCode,observedOn,fullSnapshot));
    }

    @PostMapping("/recalculate") @PreAuthorize("@authz.has('sim-investment:recalculate')")
    public ApiResponse<Map<String,Object>> recalculate(){return ApiResponse.ok(analysis.recalculate());}
}
