package digit.disbursal.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import digit.disbursal.service.DisbursalService;
import digit.disbursal.web.models.Application;
import digit.disbursal.web.models.ApplicationRequest;
import digit.disbursal.web.models.Disbursal;
import digit.disbursal.web.models.DisbursalRequest;
import lombok.extern.slf4j.Slf4j;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Consumer {

    private final ObjectMapper mapper;

    private final DisbursalService disbursalService;

    @Autowired
    public Consumer(ObjectMapper mapper, DisbursalService disbursalService) {
        this.mapper = mapper;
        this.disbursalService = disbursalService;
    }

    /*
     * Uncomment the below line to start consuming record from kafka.topics.consumer
     * Value of the variable kafka.topics.consumer should be overwritten in application.properties
     */
    @KafkaListener(topics = {"${kafka.topic.application.update}"})
    public void listen(final String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Consumer::listen");
        try {
            ApplicationRequest applicationRequest = mapper.readValue(message, ApplicationRequest.class);
            if (applicationRequest != null && applicationRequest.getApplication() != null && applicationRequest.getRequestInfo() != null) {
                Application application = applicationRequest.getApplication();
                if (application.getWfStatus().equals("APPROVED")) {
                    log.info("Workflow status is approved for the received message : " + message);
                    Disbursal disbursal = Disbursal.builder().tenantId(application.getTenantId()).referenceId(application.getId()).build();
                    DisbursalRequest  disbursalRequest = DisbursalRequest.builder().requestInfo(applicationRequest.getRequestInfo()).disbursal(disbursal).build();
                    disbursalService.create(disbursalRequest);
                } else {
                    log.info("Workflow status is not approved for the received message : " + message);
                }
            } else {
                log.info("No application request found for the received message : " + message);
            }
        } catch (Exception e) {
            log.error("Error occurred while processing the consumed save estimate record from topic : " + topic, e);
            throw new CustomException("CONSUMER_ERROR", "Error occurred while processing the consumed save estimate record from topic : " + topic);
        }

    }
}
