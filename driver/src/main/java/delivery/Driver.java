package delivery;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;
import java.util.Optional;

@Entity
@Table(name="Driver_table")
public class Driver {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long deliveryId;
    private String destination;
    private String status;
    private Integer cost;

    @PostPersist
    public void onPostPersist(){
    }
    @PostUpdate
    public void onPostUpdate() throws Exception{
        Integer settledcost = 0;

        Optional<Driver> driverOptional  = DriverApplication.applicationContext.getBean(delivery.DriverRepository.class).findById(this.getId());
        Driver driver = driverOptional.get();

        if(this.getStatus().equals("Completed")){

            try {
              settledcost = DriverApplication.applicationContext.getBean(delivery.external.PaymentsService.class).settleCost(this.getDeliveryId());
            //settledcost = DriverApplication.applicationContext.getBean(delivery.external.PaymentsService.class)
            //    .settleCost(this.getDeliveryId());
            } catch  (Exception e) {
                throw new Exception("Exception Raised when getting settledcost");
            }

        } 
        if( settledcost > 0 ){
            this.setCost( settledcost );
        }

        UpdateStatus updateStatus = new UpdateStatus();
        this.setDeliveryId(deliveryId);
        BeanUtils.copyProperties(this, updateStatus);
        updateStatus.publishAfterCommit();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
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