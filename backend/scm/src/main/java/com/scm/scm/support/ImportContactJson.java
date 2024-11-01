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

            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    ContactDTO contact = createContactFromJson(node, userToken, tenantUniqueName);
                    contacts.add(contact);
                }
            } else {
                ContactDTO contact = createContactFromJson(rootNode, userToken, tenantUniqueName);
                contacts.add(contact);
            }

            contactServices.saveAllContacts(contacts);
        } catch (Exception e) {
            throw new IOException("Napaka pri uvozu JSON datoteke", e);
        }
    }

    private ContactDTO createContactFromJson(JsonNode node, String userToken, String tenantUniqueName) {
        ContactDTO contact = new ContactDTO();
        contact.setId(UUID.randomUUID().toString());

        JsonNode propsNode = node.path("props");
        String name = getJsonFieldAsString(propsNode, "ime");
        String lastname = getJsonFieldAsString(propsNode, "priimek");

        String title = getJsonFieldAsString(node, "title");
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
        JsonNode attrsNode = node.path("attrs");
        if (attrsNode.isArray()) {
            for (JsonNode attr : attrsNode) {
                tags.add(attr.asText());
            }
        }
        contact.setTags(tags);

        JsonNode commentsNode = node.path("comments");
        contact.setComments(commentsNode.path("excel-import").asText());

        contact.setUser(userToken);
        contact.setTenantUniqueName(tenantUniqueName);
        contact.setCreatedAt(LocalDateTime.now().toString());
        contact.setAttributesToString(contact.contactAttributesToString());

        return contact;
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

    public void importRegistrationContactsFromJson(MultipartFile file, String userToken, String tenantUniqueName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ContactDTO> contacts = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(file.getInputStream());
            JsonNode typeNode = rootNode.get("type");

            if (typeNode == null) {
                throw new IOException("Manjkajoči atribut 'type' v JSON datoteki.");
            }

            String type = typeNode.asText();

            switch (type) {
                case "student":
                    ContactDTO studentContact = createStudentContact(rootNode, userToken, tenantUniqueName);
                    contacts.add(studentContact);
                    break;

                case "regular":
                    List<ContactDTO> regularContacts = createRegularContacts(rootNode, userToken, tenantUniqueName);
                    contacts.addAll(regularContacts);
                    break;

                case "group":
                    List<ContactDTO> groupContacts = createGroupContacts(rootNode, userToken, tenantUniqueName);
                    contacts.addAll(groupContacts);
                    break;

                default:
                    throw new IOException("Neznan tip JSON datoteke: " + type);
            }

            contactServices.saveAllContacts(contacts);

        } catch (Exception e) {
            throw new IOException("Napaka pri uvozu JSON datoteke", e);
        }
    }

    private ContactDTO createStudentContact(JsonNode node, String userToken, String tenantUniqueName) {
        ContactDTO contact = new ContactDTO();
        contact.setId(UUID.randomUUID().toString());

        String firstName = getJsonFieldAsString(node, "firstName");
        String lastName = getJsonFieldAsString(node, "lastName");
        contact.setTitle(createTitleFromProps(firstName, lastName));

        contact.setUser(userToken);
        contact.setTenantUniqueName(tenantUniqueName);
        contact.setCreatedAt(LocalDateTime.now().toString());

        Map<String, String> props = new HashMap<>();
        props.put("studyYear", getJsonFieldAsString(node, "studyYear"));
        props.put("studentId", getJsonFieldAsString(node, "studentId"));
        props.put("email", getJsonFieldAsString(node, "email"));
        props.put("lastName", lastName);
        props.put("firstName", firstName);
        props.put("studyLevel", getJsonFieldAsString(node, "studyLevel"));
        props.put("studyProgramme", getJsonFieldAsString(node, "studyProgramme"));

        List<String> tags = new ArrayList<>();
        JsonNode workshopsNode = node.path("workshops");
        if (workshopsNode.isArray()) {
            for (JsonNode workshop : workshopsNode) {
                if((workshop.asText()).equals("None")){
                    continue;
                }
                tags.add(workshop.asText());
            }
        }
        contact.setTags(tags);

        List<String> terms = new ArrayList<>();
        JsonNode termsNode = node.path("terms");
        if (termsNode.isArray()) {
            for (JsonNode term : termsNode) {
                terms.add(term.asText());
            }
        }
        contact.setComments(String.join(", ", terms));

        contact.setProps(props);
        contact.setAttributesToString(contact.contactAttributesToString());

        return contact;
    }

    private List<ContactDTO> createRegularContacts(JsonNode node, String userToken, String tenantUniqueName) {
        List<ContactDTO> contacts = new ArrayList<>();

        String companyName = getJsonFieldAsString(node, "companyName");
        String companyLabel = getJsonFieldAsString(node, "companyLabel");
        String companyAddress = getJsonFieldAsString(node, "companyAddress");
        String companyZip = getJsonFieldAsString(node, "companyZip");
        String companyCity = getJsonFieldAsString(node, "companyCity");
        String vatId = getJsonFieldAsString(node, "vatId");

        JsonNode participantsNode = node.path("participants");
        if (participantsNode.isArray()) {
            for (JsonNode participantNode : participantsNode) {
                ContactDTO contact = new ContactDTO();
                contact.setId(UUID.randomUUID().toString());

                Map<String, String> props = new HashMap<>();
                props.put("companyName", companyName);
                props.put("companyLabel", companyLabel);
                props.put("companyAddress", companyAddress);
                props.put("companyZip", companyZip);
                props.put("companyCity", companyCity);
                props.put("vatId", vatId);

                String firstName = getJsonFieldAsString(participantNode, "firstName");
                String lastName = getJsonFieldAsString(participantNode, "lastName");
                String email = getJsonFieldAsString(participantNode, "email");

                contact.setTitle(createTitleFromProps(firstName, lastName));
                contact.setUser(userToken);
                contact.setTenantUniqueName(tenantUniqueName);
                contact.setCreatedAt(LocalDateTime.now().toString());

                props.put("registrationType", getJsonFieldAsString(participantNode, "registrationType"));
                props.put("proceedingsFormat", getJsonFieldAsString(participantNode, "proceedingsFormat"));
                props.put("tshirt", getJsonFieldAsString(participantNode, "tshirt"));
                props.put("tshirtSize", getJsonFieldAsString(participantNode, "tshirtSize"));
                props.put("specialDietary", getJsonFieldAsString(participantNode, "specialDietary"));
                props.put("firstName", firstName);
                props.put("lastName", lastName);
                props.put("email", email);

                //Delavnice in dogodki pod tage
                List<String> tags = new ArrayList<>();
                JsonNode workshopsNode = participantNode.path("workshops");
                if (workshopsNode.isArray()) {
                    for (JsonNode workshop : workshopsNode) {
                        if((workshop.asText()).equals("None")){
                            continue;
                        }
                        tags.add(workshop.asText());
                    }
                }
                JsonNode eventsNode = participantNode.path("events");
                if (eventsNode.isArray()) {
                    for (JsonNode event : eventsNode) {
                        tags.add(event.asText());
                    }
                }
                contact.setTags(tags);

                //Dodamo pogoje (terms) pod komentarje
                List<String> terms = new ArrayList<>();
                JsonNode termsNode = participantNode.path("terms");
                if (termsNode.isArray()) {
                    for (JsonNode term : termsNode) {
                        terms.add(term.asText());
                    }
                }
                contact.setComments(String.join(", ", terms));

                contact.setProps(props);
                contact.setAttributesToString(contact.contactAttributesToString());

                contacts.add(contact);
            }
        }
        return contacts;
        }

    private List<ContactDTO> createGroupContacts(JsonNode node, String userToken, String tenantUniqueName) {
        List<ContactDTO> contacts = new ArrayList<>();

        String companyName = getJsonFieldAsString(node, "companyName");
        String companyLabel = getJsonFieldAsString(node, "companyLabel");
        String companyAddress = getJsonFieldAsString(node, "companyAddress");
        String companyZip = getJsonFieldAsString(node, "companyZip");
        String companyCity = getJsonFieldAsString(node, "companyCity");
        String vatId = getJsonFieldAsString(node, "vatId");

        JsonNode contactNode = node.path("contact");
        String contactFirstName = getJsonFieldAsString(contactNode, "firstName");
        String contactLastName = getJsonFieldAsString(contactNode, "lastName");
        String contactEmail = getJsonFieldAsString(contactNode, "email");
        String contactPhone = getJsonFieldAsString(contactNode, "phone");


        JsonNode participantsNode = node.path("participants");
        if (participantsNode.isArray()) {
            for (JsonNode participantNode : participantsNode) {
                ContactDTO contact = new ContactDTO();
                contact.setId(UUID.randomUUID().toString());

                Map<String, String> props = new HashMap<>();
                props.put("companyName", companyName);
                props.put("companyLabel", companyLabel);
                props.put("companyAddress", companyAddress);
                props.put("companyZip", companyZip);
                props.put("companyCity", companyCity);
                props.put("vatId", vatId);

                //Zdaj so pri vsakem participantu tudi podatki kontaktne osebe al kak bi blo najbolše ???
                props.put("contactFirstName", contactFirstName);
                props.put("contactLastName", contactLastName);
                props.put("contactEmail", contactEmail);
                props.put("contactPhone", contactPhone);

                String firstName = getJsonFieldAsString(participantNode, "firstName");
                String lastName = getJsonFieldAsString(participantNode, "lastName");
                String email = getJsonFieldAsString(participantNode, "email");

                contact.setTitle(createTitleFromProps(firstName, lastName));
                contact.setUser(userToken);
                contact.setTenantUniqueName(tenantUniqueName);
                contact.setCreatedAt(LocalDateTime.now().toString());

                props.put("registrationType", getJsonFieldAsString(participantNode, "registrationType"));
                props.put("proceedingsFormat", getJsonFieldAsString(participantNode, "proceedingsFormat"));
                props.put("tshirt", getJsonFieldAsString(participantNode, "tshirt"));
                props.put("tshirtSize", getJsonFieldAsString(participantNode, "tshirtSize"));
                props.put("specialDietary", getJsonFieldAsString(participantNode, "specialDietary"));
                props.put("firstName", firstName);
                props.put("lastName", lastName);
                props.put("email", email);

                //Delavnice in dogodki pod tage
                List<String> tags = new ArrayList<>();
                JsonNode workshopsNode = participantNode.path("workshops");
                if (workshopsNode.isArray()) {
                    for (JsonNode workshop : workshopsNode) {
                        if((workshop.asText()).equals("None")){
                            continue;
                        }
                        tags.add(workshop.asText());
                    }
                }
                JsonNode eventsNode = participantNode.path("events");
                if (eventsNode.isArray()) {
                    for (JsonNode event : eventsNode) {
                        tags.add(event.asText());
                    }
                }
                contact.setTags(tags);

                List<String> terms = new ArrayList<>();
                JsonNode termsNode = participantNode.path("terms");
                if (termsNode.isArray()) {
                    for (JsonNode term : termsNode) {
                        terms.add(term.asText());
                    }
                }
                contact.setComments(String.join(", ", terms));

                contact.setProps(props);
                contact.setAttributesToString(contact.contactAttributesToString());

                contacts.add(contact);
            }
        }
        return contacts;
    }

}