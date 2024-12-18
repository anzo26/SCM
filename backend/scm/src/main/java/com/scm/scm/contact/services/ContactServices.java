package com.scm.scm.contact.services;

import com.scm.scm.contact.dto.ContactDTO;
import com.scm.scm.contact.vao.Contact;
import com.scm.scm.events.services.EventsServices;
import com.scm.scm.events.vao.Event;
import com.scm.scm.events.vao.EventState;
import com.scm.scm.predefinedSearch.vao.PredefinedSearch;
import com.scm.scm.predefinedSearch.vao.SortOrientation;
import com.scm.scm.support.exceptions.CustomHttpException;
import com.scm.scm.support.exceptions.ExceptionCause;
import com.scm.scm.support.exceptions.ExceptionMessage;
import com.scm.scm.support.mongoTemplate.CollectionType;
import com.scm.scm.support.mongoTemplate.MongoTemplateService;
import com.scm.scm.tenant.services.TenantServices;
import lombok.AllArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ContactServices {

    private MongoTemplate mongoTemplate;

    private MongoTemplateService mongoTemplateService;
    private TenantServices tenantServices;
    private EventsServices eventsServices;
    private ContactServices contactServices;
    private EventsCheck eventsCheck;
    private static final Logger log = Logger.getLogger(ContactServices.class.toString());

    private static final String FOR_TENANT = " for tenant: ";

    @Autowired
    public ContactServices(MongoTemplate mongoTemplate, MongoTemplateService mongoTemplateService, EventsServices eventsServices, TenantServices tenantServices, EventsCheck eventsCheck) {
        this.eventsServices = eventsServices;
        this.eventsCheck = eventsCheck;
        this.tenantServices = tenantServices;
        this.mongoTemplate = mongoTemplate;
        this.mongoTemplateService = mongoTemplateService;
    }
    private String decodeHtmlEntities(String input) {
        return StringEscapeUtils.unescapeHtml4(input);
    }

    private ContactDTO convertToDTO(Contact contact) {
        return ContactDTO.builder()
                .id(decodeHtmlEntities(contact.getId()))
                .title(decodeHtmlEntities(contact.getTitle()))
                .user(decodeHtmlEntities(contact.getUser()))
                .tenantUniqueName(decodeHtmlEntities(contact.getTenantUniqueName()))
                .comments(decodeHtmlEntities(contact.getComments()))
                .createdAt(decodeHtmlEntities(contact.getCreatedAt().toString()))
                .tags(contact.getTags().stream().map(this::decodeHtmlEntities).collect(Collectors.toList()))
                .props(contact.getProps().entrySet().stream().collect(Collectors.toMap(
                        entry -> decodeHtmlEntities(entry.getKey()),
                        entry -> decodeHtmlEntities(entry.getValue())
                )))
                .attributesToString(decodeHtmlEntities(contact.getAttributesToString()))
                .build();
    }


    private Contact convertToEntity(ContactDTO contactDTO) {
        return new Contact(
                contactDTO.getId(),
                contactDTO.getTitle(),
                contactDTO.getUser(),
                contactDTO.getTenantUniqueName(),
                contactDTO.getComments(),
                LocalDateTime.parse(contactDTO.getCreatedAt()),
                contactDTO.getTags(),
                contactDTO.getProps(),
                contactDTO.getAttributesToString()
        );
    }

    public ContactDTO findOneContact(String tenantUniqueName, String contactId) {
        if (contactId.isEmpty() || tenantUniqueName.isEmpty()) {
            log.log(Level.WARNING, "ContactId or uniqueTenantName is empty", tenantUniqueName + " : " + contactId);
            throw new CustomHttpException("ContactId or uniqueTenantName is empty", 400, ExceptionCause.USER_ERROR);
        }
        if (!mongoTemplateService.collectionExists(tenantUniqueName + CollectionType.MAIN.getCollectionType())) {
            log.log(Level.WARNING, ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), ExceptionCause.SERVER_ERROR);
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
        }
        Contact contact = mongoTemplate.findById(contactId, Contact.class, tenantUniqueName + CollectionType.MAIN.getCollectionType());
        if (contact == null) {
            log.log(Level.WARNING, "Contact not found");
            throw new CustomHttpException("Contact not found", 404, ExceptionCause.USER_ERROR);
        }
        log.log(Level.INFO, "Contact found with id: {0}", contactId);
        return convertToDTO(contact);
    }

    public List<ContactDTO> findAllContacts(String tenantUniqueName, boolean deleted) {
        if (tenantUniqueName.isEmpty()) {
            log.log(Level.WARNING, "TenantUniqueName is empty", tenantUniqueName);
            throw new CustomHttpException(ExceptionMessage.TENANT_NAME_EMPTY.getExceptionMessage(), 400, ExceptionCause.USER_ERROR);
        }
        if (!mongoTemplateService.collectionExists(tenantUniqueName + CollectionType.MAIN.getCollectionType())) {
            log.log(Level.WARNING, "Tenant collection " + tenantUniqueName + CollectionType.MAIN.getCollectionType() + " doesn't exists.");
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
        }
        if (deleted && !mongoTemplateService.collectionExists(tenantUniqueName + CollectionType.DELETED.getCollectionType())) {
            log.log(Level.WARNING, "Tenant collection " + tenantUniqueName + CollectionType.DELETED.getCollectionType() + " doesn't exists.");
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
        }
        if(deleted){
            log.log(Level.INFO, "All deleted contacts found for tenant: {0}", tenantUniqueName);
            List<Contact> contacts = mongoTemplate.findAll(Contact.class, tenantUniqueName + CollectionType.DELETED.getCollectionType());
            return contacts.stream().map(this::convertToDTO).toList();
        }
        log.log(Level.INFO, "All contacts found for tenant: {0}", tenantUniqueName);
        List<Contact> contacts = mongoTemplate.findAll(Contact.class, tenantUniqueName + CollectionType.MAIN.getCollectionType());
        return contacts.stream().map(this::convertToDTO).toList();
    }

    public String createContact(ContactDTO contactDTO, String username, Boolean duplicateCheck) {
        ContactDTO sanitizedContactDTO = new ContactDTO();
        sanitizedContactDTO.setId(StringEscapeUtils.escapeHtml4(contactDTO.getId()));
        sanitizedContactDTO.setTitle(StringEscapeUtils.escapeHtml4(contactDTO.getTitle()));
        sanitizedContactDTO.setUser(StringEscapeUtils.escapeHtml4(contactDTO.getUser()));
        sanitizedContactDTO.setTenantUniqueName(StringEscapeUtils.escapeHtml4(contactDTO.getTenantUniqueName()));
        sanitizedContactDTO.setComments(StringEscapeUtils.escapeHtml4(contactDTO.getComments()));
        sanitizedContactDTO.setCreatedAt(StringEscapeUtils.escapeHtml4(contactDTO.getCreatedAt()));
        sanitizedContactDTO.setTags(contactDTO.getTags().stream().map(StringEscapeUtils::escapeHtml4).toList());
        sanitizedContactDTO.setProps(contactDTO.getProps().entrySet().stream().collect(Collectors.toMap(entry -> StringEscapeUtils.escapeHtml4(entry.getKey()), entry -> StringEscapeUtils.escapeHtml4(entry.getValue()))));
        sanitizedContactDTO.setAttributesToString(StringEscapeUtils.escapeHtml4(contactDTO.getAttributesToString()));
        sanitizedContactDTO.setCreatedAt(LocalDateTime.now().toString());
        Contact contact = convertToEntity(sanitizedContactDTO);
        if (contact.getTenantUniqueName().isEmpty()) {
            log.log(Level.WARNING, "TenantUniqueName is empty in contact: " + contact.getTitle(), contact);
            throw new CustomHttpException(ExceptionMessage.TENANT_NAME_EMPTY.getExceptionMessage(), 400, ExceptionCause.USER_ERROR);
        }
        if (!contact.getId().isEmpty()) {
            Contact existingContact = mongoTemplate.findById(contact.getId(), Contact.class, contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType());
            if (existingContact != null) {
                log.log(Level.WARNING, "Contact already exists!", existingContact);
                throw new CustomHttpException("Contact already exists", 400, ExceptionCause.USER_ERROR);
            }
        }
        if (!mongoTemplateService.collectionExists(contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType())) {
            log.log(Level.WARNING, "Collection not exist: " + contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType(), contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType());
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
        }
        if (contact.getTitle().isEmpty()) {
            log.log(Level.WARNING, "Contact title is empty!", contact.getTitle());
            throw new CustomHttpException("Contact title is empty", 400, ExceptionCause.USER_ERROR);
        }
        contact.setId(contact.generateId(contact.getTitle()));
        contact.setAttributesToString(contact.contactAttributesToString());
        mongoTemplate.save(contact, contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType());
        tenantServices.addTags(contact.getTenantUniqueName(), contact.getTags());
        tenantServices.addLabels(contact.getTenantUniqueName(), contact.getProps().keySet());

        Event event;
        if (duplicateCheck){
            event = new Event(username, contact.getId(), EventState.DUPLICATED);
        } else {
            event = new Event(username, contact.getId(), EventState.CREATED);
        }
        eventsServices.addEvent(event, contact.getTenantUniqueName());


        log.log(Level.INFO, String.format("Contact created with id: %s %s %s ", contact.getId(), FOR_TENANT, contact.getTenantUniqueName()));
        return "Contact created successfully to " + contact.getTenantUniqueName() + "_main collection";
    }

    public ContactDTO updateContact(ContactDTO contactDTO, String username) {
        Contact contact = convertToEntity(contactDTO);
        if (contact.getTenantUniqueName().isEmpty()) {
            log.log(Level.WARNING, "TemantUniqueName is empty in contact: " + contact.getTitle(), contact);
            throw new CustomHttpException(ExceptionMessage.TENANT_NAME_EMPTY.getExceptionMessage(), 400, ExceptionCause.USER_ERROR);
        }
        if (contact.getId().isEmpty()) {
            log.log(Level.WARNING, "Contact id is empty", contact.getId());
            throw new CustomHttpException("Contact id is empty", 400, ExceptionCause.USER_ERROR);
        }
        if (!mongoTemplateService.collectionExists(contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType())) {
            log.log(Level.WARNING, "Collection not exist: " + contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType(), contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType());
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
        }

        Contact existingContact = mongoTemplate.findById(contact.getId(), Contact.class, contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType());
        if (existingContact != null) {
            if (!existingContact.getTitle().equals(contact.getTitle())) {
                Event event = new Event();
                event.setUser(contact.getUser());
                event.setContact(existingContact.getId());
                event.setEventState(EventState.UPDATED);
                event.setPropKey("Title");
                event.setPrevState(existingContact.getTitle());
                event.setCurrentState(contact.getTitle());
                eventsServices.addEvent(event, existingContact.getTenantUniqueName());
            }
            existingContact.setTitle(contact.getTitle());
            existingContact.setComments(contact.getComments());

            eventsCheck.checkTags(existingContact, contact, username);
            existingContact.setTags(contact.getTags());

            eventsCheck.checkProps(existingContact, contact, username);
            existingContact.setProps(contact.getProps());
            existingContact.setAttributesToString(existingContact.contactAttributesToString());
            tenantServices.addLabels(existingContact.getTenantUniqueName(), contact.getProps().keySet());

            mongoTemplate.save(existingContact, existingContact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType());
            log.log(Level.INFO, String.format("Contact updated with id: %s %s %s ", contact.getId(), FOR_TENANT, contact.getTenantUniqueName()));
            return convertToDTO(existingContact);
        } else {
            log.log(Level.WARNING, "Contact does not exist!");
            throw new CustomHttpException("Contact does not exist", 500, ExceptionCause.SERVER_ERROR);
        }
    }

    public String revertContact (String tenantUniqueName, String contactId, String username) {
        if (contactId.isEmpty() || tenantUniqueName.isEmpty()) {
            log.log(Level.WARNING, "ContactId or uniqueTenantName is empty");
            throw new CustomHttpException("ContactId or uniqueTenantName is empty", 400, ExceptionCause.USER_ERROR);
        }
        if (!mongoTemplateService.collectionExists(tenantUniqueName + CollectionType.MAIN.getCollectionType()) || !mongoTemplateService.collectionExists(tenantUniqueName + CollectionType.DELETED.getCollectionType())) {
            log.log(Level.WARNING, "Collection not exist: " + tenantUniqueName + CollectionType.MAIN.getCollectionType(), tenantUniqueName + CollectionType.MAIN.getCollectionType());
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
        }
        Contact contact = mongoTemplate.findById(contactId, Contact.class, tenantUniqueName + CollectionType.DELETED.getCollectionType());
        if (contact == null ) {
            log.log(Level.WARNING, "Contact not found!");
            throw new CustomHttpException("Contact not found", 404, ExceptionCause.USER_ERROR);
        }
        mongoTemplate.remove(contact, tenantUniqueName + CollectionType.DELETED.getCollectionType());
        log.log(Level.INFO, String.format("Contact reverted with id: %s %s %s ", contact.getId(), FOR_TENANT, contact.getTenantUniqueName()));
        mongoTemplate.save(contact, tenantUniqueName + CollectionType.MAIN.getCollectionType());
        log.log(Level.INFO, "Contact saved to {} _main collection", tenantUniqueName);

        Event event = new Event(username, contact.getId(), EventState.REVERTED);
        eventsServices.addEvent(event, contact.getTenantUniqueName());

        tenantServices.addTags(tenantUniqueName, contact.getTags());

        return "Contact reverted successfully to " + tenantUniqueName + "_main collection";
    }


    public String deleteContact(String tenantUniqueName, String contactId, boolean delete, String username) {
        if (contactId.isEmpty() || tenantUniqueName.isEmpty()) {
            log.log(Level.WARNING, "ContactId or uniqueTenantName is empty");
            throw new CustomHttpException("ContactId or uniqueTenantName is empty", 400, ExceptionCause.USER_ERROR);
        }
        if (!mongoTemplateService.collectionExists(tenantUniqueName + CollectionType.MAIN.getCollectionType()) || !mongoTemplateService.collectionExists(tenantUniqueName + CollectionType.DELETED.getCollectionType())) {
            log.log(Level.WARNING, "Collection not exist: " + tenantUniqueName + CollectionType.MAIN.getCollectionType(), tenantUniqueName + CollectionType.MAIN.getCollectionType());
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
        }
        Contact contact = mongoTemplate.findById(contactId, Contact.class, tenantUniqueName + CollectionType.MAIN.getCollectionType());
        if (contact == null && !delete) {
            log.log(Level.WARNING, "Contact not found!");
            throw new CustomHttpException("Contact not found", 404, ExceptionCause.USER_ERROR);
        }
        if (delete) {
            Contact deletedContact = mongoTemplate.findById(contactId, Contact.class, tenantUniqueName + CollectionType.DELETED.getCollectionType());
            if (deletedContact == null) {
                log.log(Level.WARNING, "Contact not found in deleted collection.");
                throw new CustomHttpException("Contact not found in deleted collection", 404, ExceptionCause.USER_ERROR);
            }
            mongoTemplate.remove(deletedContact, tenantUniqueName + CollectionType.DELETED.getCollectionType());
            log.log(Level.INFO, String.format("Contact completely deleted with id: %s %s %s ", deletedContact.getId(), FOR_TENANT, deletedContact.getTenantUniqueName()));
            return "Contact deleted permanently from " + tenantUniqueName + "_deleted collection";
        }
        mongoTemplate.remove(contact, tenantUniqueName + CollectionType.MAIN.getCollectionType());
        log.log(Level.INFO, String.format("Contact deleted with id: %s %s %s ", contact.getId(), FOR_TENANT, contact.getTenantUniqueName()));
        mongoTemplate.save(contact, tenantUniqueName + CollectionType.DELETED.getCollectionType());
        log.log(Level.INFO, "Contact saved to {} _deleted collection", tenantUniqueName);

        Event event = new Event(username, contact.getId(), EventState.DELETED);
        eventsServices.addEvent(event, contact.getTenantUniqueName());

        tenantServices.removeTags(tenantUniqueName, contact.getTags());

        return "Contact deleted successfully from " + tenantUniqueName + "_main collection";
    }

    public String deleteMultipleContacts(String tenantUniqueName, List<String> contactIds, String username) {
        if (tenantUniqueName.isEmpty() || contactIds.isEmpty()) {
            log.log(Level.WARNING, "TenantUniqueName or contactIds are empty");
            throw new CustomHttpException("TenantUniqueName or contactIds are empty", 400, ExceptionCause.USER_ERROR);
        }
        if (!mongoTemplateService.collectionExists(tenantUniqueName + CollectionType.MAIN.getCollectionType())) {
            log.log(Level.WARNING, "Collection not exist: " + tenantUniqueName + CollectionType.MAIN.getCollectionType(), tenantUniqueName + CollectionType.MAIN.getCollectionType());
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
        }

        List<Contact> contacts = contactIds.stream()
                .map(id -> mongoTemplate.findById(id, Contact.class, tenantUniqueName + CollectionType.MAIN.getCollectionType()))
                .filter(Objects::nonNull)
                .toList();

        if (contacts.isEmpty()) {
            log.log(Level.WARNING, "No contacts found for the provided IDs");
            throw new CustomHttpException("No contacts found for the provided IDs", 404, ExceptionCause.USER_ERROR);
        }

        for (Contact contact : contacts) {
            mongoTemplate.remove(contact, tenantUniqueName + CollectionType.MAIN.getCollectionType());
            mongoTemplate.save(contact, tenantUniqueName + CollectionType.DELETED.getCollectionType());

            Event event = new Event(username, contact.getId(), EventState.DELETED);
            eventsServices.addEvent(event, contact.getTenantUniqueName());

            tenantServices.removeTags(tenantUniqueName, contact.getTags());
        }

        log.log(Level.INFO, "Contacts deleted and moved to deleted collection for tenant: {0}", tenantUniqueName);
        return "Contacts deleted successfully from " + tenantUniqueName + "_main collection";
    }

    public List<ContactDTO> getContactsBySearch(PredefinedSearch search) {
        if (search.getOnTenant().isEmpty()) {
            log.log(Level.WARNING, "Search query or tenant is empty", search);
            throw new CustomHttpException("Search query or tenant is empty", 400, ExceptionCause.USER_ERROR);
        }
        if (!mongoTemplateService.collectionExists(search.getOnTenant() + CollectionType.MAIN.getCollectionType())) {
            log.log(Level.WARNING, "Collection not exist: " + search.getOnTenant() + CollectionType.MAIN.getCollectionType(), search.getOnTenant() + CollectionType.MAIN.getCollectionType());
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 404, ExceptionCause.SERVER_ERROR);
        }
        if (search.getSearchQuery().isEmpty()) {
            log.log(Level.INFO, "Search query is empty!");
            List<Contact> onlyFilteredContacts = mongoTemplate.findAll(Contact.class, search.getOnTenant() + CollectionType.MAIN.getCollectionType()).stream().filter(contact -> new HashSet<>(contact.getTags()).containsAll(search.getFilter())).collect(Collectors.toList());
            Comparator<Contact> comparator = getComparatorBasedOnOrientation(search.getSortOrientation());
            onlyFilteredContacts.sort(comparator);
            return onlyFilteredContacts.stream().map(this::convertToDTO).toList();
        }
        List<Contact> allContactsByQuery = getContactsBySearchQuery(search.getSearchQuery(), mongoTemplate.findAll(Contact.class, search.getOnTenant() + CollectionType.MAIN.getCollectionType()), search.getSortOrientation());
        if (search.getFilter().isEmpty()) {
            log.log(Level.INFO, "Filter is empty!");
            return allContactsByQuery.stream().map(this::convertToDTO).toList();
        }
        log.log(Level.INFO, "Return filteredContacts!");
        List<Contact> filteredContacts = allContactsByQuery.stream().filter(contact -> new HashSet<>(contact.getTags()).containsAll(search.getFilter())).toList();
        return filteredContacts.stream().map(this::convertToDTO).toList();
    }

    public List<Contact> getContactsBySearchQuery(String searchQuery, List<Contact> contacts, SortOrientation sortOrientation) {
        if (searchQuery.contains("&")) {
            List<String> andQueries = Arrays.asList(searchQuery.split("&"));
            return contacts.stream()
                    .filter(contact -> andQueries.stream().allMatch(query -> contact.getAttributesToString().toLowerCase().contains(query.toLowerCase())))
                    .collect(Collectors.toList());
        }
        if (!searchQuery.contains("\\|")) {
            List<String> orQueries = Arrays.asList(searchQuery.split("\\|"));
            return contacts.stream()
                    .filter(contact -> orQueries.stream().anyMatch(query -> contact.getAttributesToString().toLowerCase().contains(query.toLowerCase())))
                    .collect(Collectors.toList());

        }

        List<Contact> filteredContacts = contacts.stream()
                .filter(contact -> contact.getAttributesToString().contains(searchQuery.toLowerCase()))
                .collect(Collectors.toList());

        Comparator<Contact> comparator = getComparatorBasedOnOrientation(sortOrientation);
        filteredContacts.sort(comparator);

        return filteredContacts;
    }

    public Comparator<Contact> getComparatorBasedOnOrientation(SortOrientation sortOrientation) {
        Comparator<Contact> comparator = Comparator.comparing(Contact::getTitle);
        if (sortOrientation == SortOrientation.DESC) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    public void saveAllContacts(List<ContactDTO> contacts) {
        for (ContactDTO contactDTO : contacts) {
            Contact contact = convertToEntity(contactDTO);
            if (contact.getTenantUniqueName().isEmpty()) {
                log.log(Level.WARNING, "Tenant unique name is empty", contact);
                throw new CustomHttpException(ExceptionMessage.TENANT_NAME_EMPTY.getExceptionMessage(), 400, ExceptionCause.USER_ERROR);
            }
            if (contact.getTitle().isEmpty()) {
                log.log(Level.WARNING, "Contact title is empty!", contact);
                throw new CustomHttpException("Contact title is empty", 400, ExceptionCause.USER_ERROR);
            }
            if (!mongoTemplateService.collectionExists(contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType())) {
                log.log(Level.WARNING, "Collection not exist: " + contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType(), contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType());
                throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
            }

            contact.setId(contact.generateId(contact.getTitle()));
            contact.setAttributesToString(contact.contactAttributesToString());

            mongoTemplate.save(contact, contact.getTenantUniqueName() + CollectionType.MAIN.getCollectionType());

            tenantServices.addTags(contact.getTenantUniqueName(), contact.getTags());
            tenantServices.addLabels(contact.getTenantUniqueName(), contact.getProps().keySet());

            Event event = new Event(contact.getUser(), contact.getId(), EventState.CREATED);
            eventsServices.addEvent(event, contact.getTenantUniqueName());

            log.log(Level.INFO, String.format("Contact created with id: %s for tenant: %s", contact.getId(), contact.getTenantUniqueName()));
        }
    }

    public Map<String, List<ContactDTO>> findDuplicateContactsByTitleAndEmail(String tenantUniqueName) {
        if (tenantUniqueName.isEmpty()) {
            log.log(Level.WARNING, "TenantUniqueName is empty!");
            throw new CustomHttpException(ExceptionMessage.TENANT_NAME_EMPTY.getExceptionMessage(), 400, ExceptionCause.USER_ERROR);
        }
        if (!mongoTemplateService.collectionExists(tenantUniqueName + CollectionType.MAIN.getCollectionType())) {
            log.log(Level.WARNING, "Collection not exist: " + tenantUniqueName + CollectionType.MAIN.getCollectionType(), tenantUniqueName + CollectionType.MAIN.getCollectionType());
            throw new CustomHttpException(ExceptionMessage.COLLECTION_NOT_EXIST.getExceptionMessage(), 500, ExceptionCause.SERVER_ERROR);
        }

        List<Contact> contacts = mongoTemplate.findAll(Contact.class, tenantUniqueName + CollectionType.MAIN.getCollectionType());

        Map<String, List<Contact>> potentialDuplicatesByTitle = contacts.stream()
                .collect(Collectors.groupingBy(contact -> contact.getTitle().toLowerCase()));

        Map<String, List<Contact>> potentialDuplicatesByEmail = contacts.stream()
                .filter(contact -> contact.getProps() != null && contact.getProps().containsKey("email"))
                .collect(Collectors.groupingBy(contact -> contact.getProps().get("email").toLowerCase()));

        Map<String, List<ContactDTO>> duplicates = new HashMap<>();

        potentialDuplicatesByTitle.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> duplicates.put(entry.getKey(), entry.getValue().stream().map(this::convertToDTO).collect(Collectors.toList())));

        potentialDuplicatesByEmail.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> {
                    String emailKey = entry.getKey();

                    List<ContactDTO> existingContacts = duplicates.values().stream()
                            .flatMap(List::stream)
                            .filter(dto -> entry.getValue().stream()
                                    .anyMatch(contact -> contact.getProps().get("email").equalsIgnoreCase(dto.getProps().get("email"))))
                            .collect(Collectors.toList());

                    if (existingContacts.isEmpty()) {
                        duplicates.merge(emailKey, entry.getValue().stream().map(this::convertToDTO).collect(Collectors.toList()), (existingValue, newValue) -> {
                            existingValue.addAll(newValue);
                            return existingValue;
                        });
                    }
                });

        return duplicates;
    }

    public String mergeContacts(String targetContactId, String sourceContactId, String tenantUniqueName, String username) {
        Contact targetContact = mongoTemplate.findById(targetContactId, Contact.class, tenantUniqueName + CollectionType.MAIN.getCollectionType());
        Contact sourceContact = mongoTemplate.findById(sourceContactId, Contact.class, tenantUniqueName + CollectionType.MAIN.getCollectionType());

        if (targetContact == null || sourceContact == null) {
            log.log(Level.WARNING, "One or both contacts not found");
            throw new CustomHttpException("One or both contacts not found", 404, ExceptionCause.USER_ERROR);
        }

        List<String> mergedTags = new ArrayList<>(targetContact.getTags());
        for (String tag : sourceContact.getTags()) {
            if (!mergedTags.contains(tag)) {
                mergedTags.add(tag);
            }
        }
        eventsCheck.checkTagsMerging(targetContact, sourceContact, username);
        targetContact.setTags(mergedTags);

        Map<String, String> mergedProps = new HashMap<>(targetContact.getProps());
        for (Map.Entry<String, String> entry : sourceContact.getProps().entrySet()) {
            if (!mergedProps.containsKey(entry.getKey())) {
                mergedProps.put(entry.getKey(), entry.getValue());
            }
        }
        eventsCheck.checkPropsMerging(targetContact, sourceContact, username);
        targetContact.setProps(mergedProps);

        mongoTemplate.save(targetContact, tenantUniqueName + CollectionType.MAIN.getCollectionType());

        mongoTemplate.remove(sourceContact, tenantUniqueName + CollectionType.MAIN.getCollectionType());
        log.log(Level.INFO, "Contacts merged successfully!");

        return "Contacts merged successfully";
    }

}