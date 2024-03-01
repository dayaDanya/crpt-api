package org.goncharov;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    public static void main(String[] args) throws InterruptedException {
        Description description = new Description("123456789");
        List<Product> products = new ArrayList<>();
        products.add(new Product("cert1", "2024-02-28", "12345", "owner_inn1", "producer_inn1",
                "2024-01-01", "tnved_code1", "uit_code1", "uitu_code1"));
        products.add(new Product("cert2", "2024-02-27", "54321", "owner_inn2", "producer_inn2",
                "2024-01-02", "tnved_code2", "uit_code2", "uitu_code2"));
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 1);
        for (int i = 0; i < 10; i++) {
            Document document = new Document(description, "doc_id1", "status1", "type1",
                    true, "owner_inn", "participant_inn", "producer_inn",
                    "2024-02-28", "production_type1", products, "2024-03-01", "reg_number1");
            document.doc_id = String.valueOf(i);
            crptApi.create(document);
        }


    }
    private final TimeUnit timeUnit;
    //todo добавить валидацию, что число в рамках инт
    private final int requestLimit;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Queue<Document> requestQueue;
    private ScheduledExecutorService scheduler;

    private final String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.requestQueue = new LinkedList<>();
    }

    public synchronized void create(Document doc) {
        if(scheduler == null){
            scheduler = Executors.newScheduledThreadPool(1);
            start();
        }
        requestQueue.offer(doc);
        System.out.println("Request added to queue: " + doc);
    }

    private void start() {
        scheduler.scheduleAtFixedRate(this::processQueue, 0, 1, timeUnit);
    }


    private void processQueue() {
        int processedRequests = 0;
        while (!requestQueue.isEmpty() && processedRequests < requestLimit) {
            processRequest(requestQueue.poll());
            processedRequests++;
        }
        if (requestQueue.isEmpty()){
            scheduler.shutdown();
            scheduler = null;
        }
    }

    private void processRequest(Document dto) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(dto)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
        System.out.println("Processing request: " + dto);
    }

    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        public Document(Description description, String doc_id,
                        String doc_status, String doc_type, boolean importRequest,
                        String owner_inn, String participant_inn,
                        String producer_inn, String production_date,
                        String production_type, List<Product> products,
                        String reg_date, String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }

        public Document() {
        }

        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDoc_id() {
            return doc_id;
        }

        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

        public String getDoc_status() {
            return doc_status;
        }

        public void setDoc_status(String doc_status) {
            this.doc_status = doc_status;
        }

        public String getDoc_type() {
            return doc_type;
        }

        public void setDoc_type(String doc_type) {
            this.doc_type = doc_type;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getParticipant_inn() {
            return participant_inn;
        }

        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getProduction_type() {
            return production_type;
        }

        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public String getReg_date() {
            return reg_date;
        }

        public void setReg_date(String reg_date) {
            this.reg_date = reg_date;
        }

        public String getReg_number() {
            return reg_number;
        }

        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }

        @Override
        public String toString() {
            return "Document{" +
                    "doc_id='" + doc_id + '\'' +
                    '}';
        }
    }

    static class Description {
        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }

    }

    static class Product {

        public Product(String certificate_document, String certificate_document_date,
                       String certificate_document_number, String owner_inn,
                       String producer_inn, String production_date, String tnved_code,
                       String uit_code, String uitu_code) {
            this.certificate_document = certificate_document;
            this.certificate_document_date = certificate_document_date;
            this.certificate_document_number = certificate_document_number;
            this.owner_inn = owner_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.tnved_code = tnved_code;
            this.uit_code = uit_code;
            this.uitu_code = uitu_code;
        }

        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        public String getCertificate_document() {
            return certificate_document;
        }

        public void setCertificate_document(String certificate_document) {
            this.certificate_document = certificate_document;
        }

        public String getCertificate_document_date() {
            return certificate_document_date;
        }

        public void setCertificate_document_date(String certificate_document_date) {
            this.certificate_document_date = certificate_document_date;
        }

        public String getCertificate_document_number() {
            return certificate_document_number;
        }

        public void setCertificate_document_number(String certificate_document_number) {
            this.certificate_document_number = certificate_document_number;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getTnved_code() {
            return tnved_code;
        }

        public void setTnved_code(String tnved_code) {
            this.tnved_code = tnved_code;
        }

        public String getUit_code() {
            return uit_code;
        }

        public void setUit_code(String uit_code) {
            this.uit_code = uit_code;
        }

        public String getUitu_code() {
            return uitu_code;
        }

        public void setUitu_code(String uitu_code) {
            this.uitu_code = uitu_code;
        }
    }

}
