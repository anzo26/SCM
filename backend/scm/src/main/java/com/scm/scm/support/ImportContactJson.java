package com.scm.scm.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scm.scm.contact.dto.ContactDTO;
import com.scm.scm.contact.services.ContactServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ImportContactJson {

    private final ContactServices contactServices;

    private static final Map<String, String> SLO_ENG_MAPPING = Map.ofEntries(
            Map.entry("predpona", "prefix"),
            Map.entry("ime", "name"),
            Map.entry("priimek", "lastname"),
            Map.entry("ulica", "address"),
            Map.entry("podjetje", "company"),
            Map.entry("hisnaStevilka", "houseNumber"),
            Map.entry("postnaStevilka", "postNumber"),
            Map.entry("posta", "city"),
            Map.entry("elektronskiNaslov", "email")
    );

    @Autowired
    public ImportContactJson(ContactServices contactServices) {
        this.contactServices = contactServices;
    }

    public void importContactsFromJson(MultipartFile file, String userToken, String tenantUniqueName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ContactDTO> contacts = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(file.getInputStream());
            ContactDTO contact = new ContactDTO();
            contact.setId(UUID.randomUUID().toString());

            JsonNode propsNode = rootNode.path("props");
            String name = getJsonFieldAsString(propsNode, "ime");
            String lastname = getJsonFieldAsString(propsNode, "priimek");

            String title = getJsonFieldAsString(rootNode, "title");
            contact.setTitle(title.isEmpty() ? createTitleFromProps(name, lastname) : title);

            Map<String, String> props = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = propsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                String value = field.getValue().asText();

                String mappedField = SLO_ENG_MAPPING.getOrDefault(fieldName, fieldName);

                if (mappedField.equals("name") || mappedField.equals("lastname")) {
                    continue;
                }

                if (!value.isEmpty()) {
                    props.put(mappedField, value);
                }
            }
            contact.setProps(props);

            List<String> tags = new ArrayList<>();
            JsonNode attrsNode = rootNode.path("attrs");
            if (attrsNode.isArray()) {
                for (JsonNode attr : attrsNode) {
                    tags.add(attr.asText());
                }
            }
            contact.setTags(tags);

            JsonNode commentsNode = rootNode.path("comments");
            contact.setComments(commentsNode.path("excel-import").asText());

            contact.setUser(userToken);
            contact.setTenantUniqueName(tenantUniqueName);
            contact.setCreatedAt(LocalDateTime.now().toString());
            contact.setAttributesToString(contact.contactAttributesToString());

            contacts.add(contact);

            contactServices.saveAllContacts(contacts);
        } catch (Exception e) {
            throw new IOException("Napaka pri uvozu JSON datoteke", e);
        }
    }

    private String getJsonFieldAsString(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.path(fieldName).asText() : "";
    }

    private String createTitleFromProps( String name, String lastname) {
        StringBuilder title = new StringBuilder();
        if (!name.isEmpty()) {
            title.append(name).append(" ");
        }
        if (!lastname.isEmpty()) {
            title.append(lastname);
        }
        return title.toString().trim().isEmpty() ? "Contact" : title.toString().trim();
    }
}