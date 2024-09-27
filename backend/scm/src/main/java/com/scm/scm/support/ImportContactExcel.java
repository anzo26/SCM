package com.scm.scm.support;

import com.scm.scm.contact.dto.ContactDTO;
import com.scm.scm.contact.services.ContactServices;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

@Service
public class ImportContactExcel {

    private static final Logger log = Logger.getLogger(ImportContactExcel.class.toString());

    private final ContactServices contactServices;

    private static final Map<String, String> COLUMN_NAME_MAPPING = Map.ofEntries(
            Map.entry("predpona", "prefix"),
            Map.entry("ime", "name"),
            Map.entry("priimek", "lastname"),
            Map.entry("podjetje", "company"),
            Map.entry("ulica", "address"),
            Map.entry("hišna številka", "houseNumber"),
            Map.entry("poštna številka", "postNumber"),
            Map.entry("pošta", "city"),
            Map.entry("država", "country"),
            Map.entry("elektronski naslov", "email"),
            Map.entry("telefonska", "phoneNumber")
    );

    @Autowired
    public ImportContactExcel(ContactServices contactServices) {
        this.contactServices = contactServices;
    }

    public void importContactsFromExcel(MultipartFile file, String userToken, String tenantUniqueName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<ContactDTO> contacts = new ArrayList<>();

            Row headerRow = sheet.getRow(0);
            Map<Integer, String> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String columnName = getMappedColumnName(cell.getStringCellValue());
                headerMap.put(cell.getColumnIndex(), columnName);
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }

                ContactDTO contact = new ContactDTO();
                contact.setId(UUID.randomUUID().toString());

                String name = "";
                String lastname = "";
                String title = "";

                try {
                    name = getCellValueAsString(row.getCell(getColumnIndex(headerMap, "name", false)));
                } catch (Exception ignored) {}

                try {
                    lastname = getCellValueAsString(row.getCell(getColumnIndex(headerMap, "lastname", false)));
                } catch (Exception ignored) {}

                try {
                    title = getCellValueAsString(row.getCell(getColumnIndex(headerMap, "title", false)));
                } catch (Exception ignored) {}

                if (!title.isEmpty()) {
                    contact.setTitle(title);
                } else if (!name.isEmpty() && !lastname.isEmpty()) {
                    contact.setTitle(name + " " + lastname);
                } else if (!name.isEmpty()) {
                    contact.setTitle(name);
                } else if (!lastname.isEmpty()) {
                    contact.setTitle(lastname);
                } else {
                    contact.setTitle("Contact");
                }

                contact.setUser(userToken);
                contact.setTenantUniqueName(tenantUniqueName);

                Map<String, String> props = new HashMap<>();
                List<String> tags = new ArrayList<>();
                List<String> predefinedProps = Arrays.asList("prefix", "company", "email", "phoneNumber", "houseNumber", "address", "postNumber", "city", "country", "comment");

                for (String prop : predefinedProps) {
                    try {
                        String value = getCellValueAsString(row.getCell(getColumnIndex(headerMap, prop, true)));
                        if (value != null && !value.isEmpty()) {
                            props.put(prop, value);
                        }
                    } catch (Exception ignored) {}
                }

                contact.setCreatedAt(LocalDateTime.now().toString());

                for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
                    String columnName = entry.getValue();
                    if (columnName.equals("name") || columnName.equals("lastname") || predefinedProps.contains(columnName)) {
                        continue;
                    }

                    String value = getCellValueAsString(row.getCell(entry.getKey()));
                    if (Objects.equals(value, "")) {
                        continue;
                    }

                    if (columnName.equalsIgnoreCase(value)) {
                        tags.add(columnName);
                    } else {
                        props.put(columnName, value);
                    }
                }

                contact.setTags(tags);
                contact.setProps(props);
                contact.setAttributesToString(contact.contactAttributesToString());

                contacts.add(contact);
            }

            contactServices.saveAllContacts(contacts);
        } catch (Exception e) {
            log.severe("Napaka pri uvozu: " + e.getMessage());
            throw new IOException("Napaka pri uvozu", e);
        }
    }

    private Integer getColumnIndex(Map<Integer, String> headerMap, String columnName, boolean returnNullIfNotFound) {
        return headerMap.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(columnName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private String getMappedColumnName(String originalColumnName) {
        return COLUMN_NAME_MAPPING.getOrDefault(originalColumnName.toLowerCase(), originalColumnName);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}