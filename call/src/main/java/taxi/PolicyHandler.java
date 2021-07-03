package taxi;

import taxi.config.kafka.KafkaProcessor;

import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired CallRepository callRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUpdatedStatus_UpdateStatus(@Payload UpdatedStatus updatedStatus){

        if(!updatedStatus.validate()) return;

        System.out.println("\n\n##### listener UpdateStatus : " + updatedStatus.toJson() + "\n\n");

        // Sample Logic //
        // Call call = new Call();

        Optional<Call> callOptional =  callRepository.findById(updatedStatus.getCallId());
        Call call = callOptional.get();
        call.setStatus(updatedStatus.getStatus());

        callRepository.save(call);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
