package com.example.service;

import com.example.dto.ClientSummaryView;
import com.example.dto.ExpiringDocumentView;
import com.example.model.Client;
import com.example.repository.ClientRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional
    public Client createClient(Client client) {
        Client saved = clientRepository.save(client);
        log.info("Client created: clientId={} clientType={} status={}",
                saved.getClientId(), saved.getClientType(), saved.getStatus());
        return saved;
    }

    @Transactional
    public boolean updateStatus(Integer clientId, String status) {
        int updated = clientRepository.updateStatus(clientId, status);
        if (updated > 0) {
            log.info("Client status updated: clientId={} status={}", clientId, status);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<ClientSummaryView> listClients() {
        return clientRepository.findAllSummaries();
    }

    @Transactional(readOnly = true)
    public List<Client> listClients() {
        return clientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Client> getClientById(Integer id) {
        Optional<Client> client = clientRepository.findById(id);
        if (client.isEmpty()) {
            log.debug("Client not found: clientId={}", id);
        }
        return client;
    }

    @Transactional(readOnly = true)
    public List<ExpiringDocumentView> listExpiringDocuments(int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);
        List<ExpiringDocumentView> docs = clientRepository.findExpiringDocuments(today, endDate);

        docs.forEach(doc -> log.warn(
                "Document expiring in {} days: clientId={} docType={} expiry={}",
                days, doc.getClientId(), doc.getDocType(), doc.getExpiryDate()));

        return docs;
    }
}