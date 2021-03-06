package au.id.keen.pfm.controller;

import au.id.keen.pfm.dto.DailySummary;
import au.id.keen.pfm.dto.JobStatusDto;
import au.id.keen.pfm.enums.DownloadFormatEnum;
import au.id.keen.pfm.service.TransactionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/upload")
    public JobStatusDto postUploadFile(@RequestParam(name = "file") MultipartFile pFile) throws IOException {
        return transactionService.processFile(pFile);
    }

    @GetMapping("/job/{pJobId}")
    public JobStatusDto getJobStatus(@PathVariable Long pJobId) {
        return transactionService.getJobStatus(pJobId);
    }

    @GetMapping("/summary/{pJobId}")
    public ResponseEntity<?> getSummary(@PathVariable Long pJobId,
                                        @RequestParam(required = false, name = "format") DownloadFormatEnum pFormat) {
        List<DailySummary> summaries = transactionService.getSummaryRecords(pJobId);
        return getResponse(summaries, pFormat);
    }

    private ResponseEntity<?> getResponse(List<DailySummary> pSummaries, DownloadFormatEnum pFormat) {
        if (pFormat == null) { // return JSON
            return ResponseEntity.ok(pSummaries);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, pFormat.getContentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Output.".concat(pFormat.toString()))
                .body(new ByteArrayResource(pFormat.getToByteArray().apply(pSummaries)));
    }

}
