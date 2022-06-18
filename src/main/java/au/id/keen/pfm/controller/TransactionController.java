package au.id.keen.pfm.controller;

import au.id.keen.pfm.dto.DailySummaryDto;
import au.id.keen.pfm.dto.JobStatusDto;
import au.id.keen.pfm.enums.DownloadFormatEnum;
import au.id.keen.pfm.service.TransactionService;
import au.id.keen.pfm.util.FileUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
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

    private final JobLauncher jobLauncher;
    private final JobLauncher asyncJobLauncher;
    private final Job uploadJob;
    private final JobExplorer jobExplorer;
    private final TransactionService transactionService;

    public TransactionController(JobLauncher jobLauncher, JobLauncher asyncJobLauncher, Job uploadJob, JobExplorer jobExplorer, TransactionService transactionService) {
        this.jobLauncher = jobLauncher;
        this.asyncJobLauncher = asyncJobLauncher;
        this.uploadJob = uploadJob;
        this.jobExplorer = jobExplorer;
        this.transactionService = transactionService;
    }

    @PostMapping("/upload")
    public JobStatusDto upload(@RequestParam MultipartFile file) throws IOException {

/*        File tempFile = File.createTempFile("upl", null);

        tempFile.deleteOnExit();

        file.transferTo(tempFile);*/

        /*file.getResource().getFile().getAbsolutePath();

        // async run job and delete temp file
        Files.createTempFile()*/

        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("file.path", FileUtils.getTempFile(file).getAbsolutePath());
        // add date time stamp
//        builder.addString("dateTime", LocalDateTime.now());

        try {
            JobExecution execution = jobLauncher.run(uploadJob, builder.toJobParameters());
            return new JobStatusDto(execution.getJobId(), execution.getStatus().toString(), null);
        } catch (JobExecutionAlreadyRunningException e) {
            e.printStackTrace();
        } catch (JobRestartException e) {
            e.printStackTrace();
        } catch (JobInstanceAlreadyCompleteException e) {
            e.printStackTrace();
        } catch (JobParametersInvalidException e) {
            e.printStackTrace();
        }

        /*while (job ! finished) {

        }*/

//        tempFile.delete();

        return new JobStatusDto(null, null, null);
    }

    @GetMapping("/job/{pId}")
    public JobStatusDto job(@PathVariable Long pId) {
        JobExecution execution = jobExplorer.getJobExecution(pId);
        if (execution != null) {
            return new JobStatusDto(pId, execution.getStatus().toString(), null);
        }
        return new JobStatusDto(pId, null, "Job ID not found");
    }

    @GetMapping("/summary/{pJobId}")
    public ResponseEntity<?> getSummary(@PathVariable Long pJobId,
                                        @RequestParam(required = false) DownloadFormatEnum format) {
        List<DailySummaryDto> summaries = transactionService.getRecords(pJobId);
        return getResponse(summaries, format);
    }

    private ResponseEntity<?> getResponse(List<DailySummaryDto> pSummaries, DownloadFormatEnum pFormat) {
        if (pFormat == null) {
            return ResponseEntity.ok(pSummaries);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, pFormat.getContentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Output.".concat(pFormat.toString()))
                .body(new ByteArrayResource(pFormat.getToByteArray().apply(pSummaries)));
    }

}