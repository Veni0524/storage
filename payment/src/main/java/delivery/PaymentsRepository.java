package delivery;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="payments", path="payments")
public interface PaymentsRepository extends PagingAndSortingRepository<Payments, Long>{

    Optional<Payments> findById(Long id);
}
