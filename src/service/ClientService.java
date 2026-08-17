package service;

import repository.ClientRepository;
import java.sql.SQLException;
import java.util.List;

public class ClientService {
    private final ClientRepository clientRepository = new ClientRepository();

    public int createClient() throws SQLException {
        return clientRepository.createClient("Jan Kowalski", "INDIVIDUAL", "PL", "PL", "1990-01-01", "PL", "PENDING", true);
    }

    public String listClients() throws SQLException {
        List<String> clients = clientRepository.listClients();
        return "[\n" + String.join(",\n", clients) + "\n]";
    }

    public String listExpiringDocuments(int days) throws SQLException {
        List<String> docs = clientRepository.listExpiringDocuments(days);
        return "[\n" + String.join(",\n", docs) + "\n]";
    }

    public String getClientById(int id) throws SQLException {
        return clientRepository.getClientById(id);
    }
}