package taxi;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Call_table")
public class Call {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long customerId;
    private String destination;
    private String status;
    private Integer cost;

    @PostPersist
    public void onPostPersist(){

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        taxi.external.Payment payment = new taxi.external.Payment();
        // mappings goes here
        if( this.getStatus() == null ){
            this.setStatus("Called");
        }

        payment.setCallId(this.getId());
        payment.setCost(this.getCost());
        payment.setDestination(this.getDestination());
        
        CallApplication.applicationContext.getBean(taxi.external.PaymentService.class)
            .savePayment(payment);

        CalledTaxi calledTaxi = new CalledTaxi();
        BeanUtils.copyProperties(this, calledTaxi);
        calledTaxi.publishAfterCommit();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
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
