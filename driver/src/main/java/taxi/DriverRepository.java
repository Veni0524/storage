package taxi;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="drivers", path="drivers")
public interface DriverRepository extends PagingAndSortingRepository<Driver, Long>{

    Optional<Driver> findById(Long id);

}
