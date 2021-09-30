package delivery;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Delivery_table")
public class Delivery {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long customerId;
    private String destination;
    private String status;
    private Integer cost;

    @PostPersist
    public void onPostPersist(){

        delivery.external.Payments payments = new delivery.external.Payments();

        if ( this.getStatus() == null){
            this.setStatus("Called");
        }
        
        payments.setDeliveryId(this.getId());
        payments.setCost(this.getCost());
        payments.setDestination(this.getDestination());

        DeliveryApplication.applicationContext.getBean(delivery.external.PaymentsService.class).savePayments(payments);

        CalledDriver calledDriver = new CalledDriver();
        BeanUtils.copyProperties(this, calledDriver);
        calledDriver.publishAfterCommit(); 

    }
    @PrePersist
    public void onPrePersist(){
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