package taxi;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;

import java.util.List;
import java.util.Optional;
import java.util.Date;

@Entity
@Table(name="Driver_table")
public class Driver {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long callId;
    private String destination;
    private String status;
    private Integer cost;

    @PostUpdate
    public void onPostUpdate() throws Exception {
        // UpdatedStatus updatedStatus = new UpdatedStatus();
        // BeanUtils.copyProperties(this, updatedStatus);
        // updatedStatus.publishAfterCommit();

        // //Following code causes dependency to external APIs
        // // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        // taxi.external.Payment payment = new taxi.external.Payment();
        // // mappings goes here
        // DriverApplication.applicationContext.getBean(taxi.external.PaymentService.class)
        //     .settleCost(payment);
        Integer settledcost = 0;

        Optional<Driver> driverOptional  = DriverApplication.applicationContext.getBean(taxi.DriverRepository.class).findById(this.getId());
        Driver driver = driverOptional.get();

        if(this.getStatus().equals("Completed")){

            try {
            settledcost = DriverApplication.applicationContext.getBean(taxi.external.PaymentService.class)
                .settleCost(this.getCallId());
            } catch  (Exception e) {
                throw new Exception("Exception Raised when getting settledcost");
            }

        } 
        if( settledcost > 0 ){
            this.setCost( settledcost );
        }

        UpdatedStatus updatedStatus = new UpdatedStatus();
        this.setCallId(callId);
        BeanUtils.copyProperties(this, updatedStatus);
        updatedStatus.publishAfterCommit();




    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getCallId() {
        return callId;
    }

    public void setCallId(Long callId) {
        this.callId = callId;
    }
    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

}
