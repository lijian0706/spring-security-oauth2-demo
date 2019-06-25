package com.lijian.authserver.repository;

import com.lijian.authserver.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @Author: Lijian
 * @Date: 2019-06-25 14:39
 */
public interface ClientRepository extends JpaRepository<Client, Long> {
    Client findByClientId(String clientId);
}
