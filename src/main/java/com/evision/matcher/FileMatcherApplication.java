package com.evision.matcher;

import com.evision.matcher.model.MatchResult;
import com.evision.matcher.service.MatchingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.List;

@Slf4j
@SpringBootApplication
public class FileMatcherApplication {

    public static void main(String[] args) {

        ApplicationContext context = SpringApplication.run(FileMatcherApplication.class, args);
        MatchingService service = context.getBean(MatchingService.class);
        List<MatchResult> results = null;
        try {
            results = service.matchAll();
        }
        catch (Exception e) {
            log.error("Error during matching", e);
            e.printStackTrace();
            return;
        }
        for (int i = 0; i < results.size(); i++) {
            MatchResult r = results.get(i);
            log.info(String.format("%-30s %7.2f%%%s",
                    r.fileName(), r.score(), i == 0 ? "   <-- best match" : ""));
        }
    }
}
