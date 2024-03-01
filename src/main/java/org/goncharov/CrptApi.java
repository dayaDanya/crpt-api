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
        CrptApi crptApi = CrptApiBuilder.build(TimeUnit.SECONDS, 10);
        for (int i = 0; i < 10; i++) {
            Document document = new Document(description, "doc_id1", "status1", "type1",
                    true, "owner_inn", "participant_inn", "producer_inn",
                    "2024-02-28", "production_type1", products, "2024-03-01", "reg_number1");
            document.doc_id = String.valueOf(i);
            crptApi.create(document);
        }
    }

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Queue<Document> requestQueue;
    private ScheduledExecutorService scheduler;

    private final String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";


    /**
     * Конструктор CrptApi.
     *
     * @param timeUnit     Единица измерения времени для периода выполнения задач в планировщике.
     * @param requestLimit Максимальное количество запросов, которое может быть обработано в единицу времени.
     */
    private CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.requestQueue = new LinkedList<>();
    }

    /**
     * Добавляет документ в очередь запросов.
     * Если планировщик равен null, он инициализируется и запускается.
     *
     * @param doc Документ для добавления в очередь запросов.
     */
    public synchronized void create(Document doc) {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
            start();
        }
        requestQueue.offer(doc);
        System.out.println("Request added to queue: " + doc);
    }

    /**
     * Запускает выполнение задачи по обработке очереди запросов с заданным интервалом времени.
     */
    private void start() {
        scheduler.scheduleAtFixedRate(this::processQueue, 0, 1, timeUnit);
    }

    /**
     * Обрабатывает элементы очереди запросов.
     * В случае если очередь пустая, планироващик останавливается и объект присваивается null.
     */
    private void processQueue() {
        int processedRequests = 0;
        while (!requestQueue.isEmpty() && processedRequests < requestLimit) {
            processRequest(requestQueue.poll());
            processedRequests++;
        }
        if (requestQueue.isEmpty()) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    /**
     * Обрабатывает запрос.
     *
     * @param doc Документ передаваемый в запрос.
     * @exception IOException в случае выбрасывания исключения в методе send() ли writeValueAsString()
     * @exception InterruptedException в случае прерывания работы метода send()
     */
    private void processRequest(Document doc) {
        try {
            System.out.println("Processing request: " + doc);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(doc)))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builder предоставляющий экземпляр класса CrptApi.
     */
    public static final class CrptApiBuilder {
        /**
         * Создает новый экземпляр CrptApiBuilder.
         */
        public CrptApiBuilder() {
        }

        /**
         * Статический метод для создания экземпляра CrptApi с заданными параметрами.
         *
         * @param timeUnit     Единица измерения времени для периода выполнения задач в планировщике.
         * @param requestLimit Максимальное количество запросов, которое может быть обработано в единицу времени.
         * @return Новый экземпляр CrptApi.
         * @throws IllegalArgumentException Если значение количества запросов меньше 1.
         */
        public static CrptApi build(TimeUnit timeUnit, int requestLimit) {
            if (requestLimit < 1) {
                throw new IllegalArgumentException("Request limit value must be larger than 0. Given request limit is: " + requestLimit);
            }
            return new CrptApi(timeUnit, requestLimit);
        }
    }

    /**
     * Класс, представляющий документ.
     */
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

        /**
         * Конструктор класса Document.
         *
         * @param description     Описание документа.
         * @param doc_id          Идентификатор документа.
         * @param doc_status      Статус документа.
         * @param doc_type        Тип документа.
         * @param importRequest   Флаг импортного запроса.
         * @param owner_inn       ИНН владельца.
         * @param participant_inn ИНН участника.
         * @param producer_inn    ИНН производителя.
         * @param production_date Дата производства.
         * @param production_type Тип производства.
         * @param products        Список продуктов.
         * @param reg_date        Дата регистрации.
         * @param reg_number      Регистрационный номер.
         */
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

        /**
         * Пустой конструктор класса Document.
         */
        public Document() {
        }

        /**
         * Получает идентификатор документа.
         *
         * @return Идентификатор документа.
         */
        public String getDoc_id() {
            return doc_id;
        }

        /**
         * Устанавливает идентификатор документа.
         *
         * @param doc_id Идентификатор документа.
         */
        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

        /**
         * Получает статус документа.
         *
         * @return Статус документа.
         */
        public String getDoc_status() {
            return doc_status;
        }

        /**
         * Устанавливает статус документа.
         *
         * @param doc_status Статус документа.
         */
        public void setDoc_status(String doc_status) {
            this.doc_status = doc_status;
        }

        /**
         * Получает тип документа.
         *
         * @return Тип документа.
         */
        public String getDoc_type() {
            return doc_type;
        }

        /**
         * Устанавливает тип документа.
         *
         * @param doc_type Тип документа.
         */
        public void setDoc_type(String doc_type) {
            this.doc_type = doc_type;
        }

        /**
         * Проверяет, является ли документ импортным запросом.
         *
         * @return true, если документ является импортным запросом, иначе - false.
         */
        public boolean isImportRequest() {
            return importRequest;
        }

        /**
         * Устанавливает флаг импортного запроса.
         *
         * @param importRequest Флаг импортного запроса.
         */
        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        /**
         * Получает ИНН владельца.
         *
         * @return ИНН владельца.
         */
        public String getOwner_inn() {
            return owner_inn;
        }

        /**
         * Устанавливает ИНН владельца.
         *
         * @param owner_inn ИНН владельца.
         */
        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        /**
         * Получает ИНН участника.
         *
         * @return ИНН участника.
         */
        public String getParticipant_inn() {
            return participant_inn;
        }

        /**
         * Устанавливает ИНН участника.
         *
         * @param participant_inn ИНН участника.
         */
        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        /**
         * Получает ИНН производителя.
         *
         * @return ИНН производителя.
         */
        public String getProducer_inn() {
            return producer_inn;
        }

        /**
         * Устанавливает ИНН производителя.
         *
         * @param producer_inn ИНН производителя.
         */
        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        /**
         * Получает дату производства.
         *
         * @return Дата производства.
         */
        public String getProduction_date() {
            return production_date;
        }

        /**
         * Устанавливает дату производства.
         *
         * @param production_date Дата производства.
         */
        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        /**
         * Получает тип производства.
         *
         * @return Тип производства.
         */
        public String getProduction_type() {
            return production_type;
        }

        /**
         * Устанавливает тип производства.
         *
         * @param production_type Тип производства.
         */
        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        /**
         * Получает список продуктов.
         *
         * @return Список продуктов.
         */
        public List<Product> getProducts() {
            return products;
        }

        /**
         * Устанавливает список продуктов.
         *
         * @param products Список продуктов.
         */
        public void setProducts(List<Product> products) {
            this.products = products;
        }

        /**
         * Получает дату регистрации.
         *
         * @return Дата регистрации.
         */
        public String getReg_date() {
            return reg_date;
        }

        /**
         * Устанавливает дату регистрации.
         *
         * @param reg_date Дата регистрации.
         */
        public void setReg_date(String reg_date) {
            this.reg_date = reg_date;
        }

        /**
         * Получает регистрационный номер.
         *
         * @return Регистрационный номер.
         */
        public String getReg_number() {
            return reg_number;
        }

        /**
         * Устанавливает регистрационный номер.
         *
         * @param reg_number Регистрационный номер.
         */
        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }

        /**
         * Возвращает строковое представление документа(номер)
         * @return строковое представление документа(номер)
         */
        @Override
        public String toString() {
            return "Document{" +
                    "doc_id='" + doc_id + '\'' +
                    '}';
        }
    }

    /**
     * Класс, представляющий описание документа.
     */
    static class Description {
        private String participantInn;

        /**
         * Получает ИНН участника.
         *
         * @return ИНН участника.
         */
        public String getParticipantInn() {
            return participantInn;
        }

        /**
         * Устанавливает ИНН участника.
         *
         * @param participantInn ИНН участника.
         */
        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        /**
         * Конструктор класса Description.
         *
         * @param participantInn ИНН участника.
         */
        public Description(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    /**
     * Представляет продукт.
     */
    static class Product {

        /**
         * Создает новый экземпляр с указанными характеристиками.
         *
         * @param certificate Сертификат.
         * @param certificate_date Дата сертификата.
         * @param certificate_number Номер сертификата.
         * @param owner_inn ИНН владельца.
         * @param producer_inn ИНН производителя.
         * @param production_date Дата производства.
         * @param tnved_code Код ТН ВЭД.
         * @param uit_code Код УИТ.
         * @param uitu_code Код УИТУ.
         */
        public Product(String certificate, String certificate_date,
                       String certificate_number, String owner_inn,
                       String producer_inn, String production_date, String tnved_code,
                       String uit_code, String uitu_code) {
            this.certificate_document = certificate;
            this.certificate_document_date = certificate_date;
            this.certificate_document_number = certificate_number;
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

        /**
         * Получает сертификат.
         *
         * @return Сертификат.
         */
        public String getCertificate_document() {
            return certificate_document;
        }

        /**
         * Устанавливает сертификат.
         *
         * @param certificate_document Сертификат.
         */
        public void setCertificate_document(String certificate_document) {
            this.certificate_document = certificate_document;
        }

        /**
         * Получает дату сертификата.
         *
         * @return Дата сертификата.
         */
        public String getCertificate_document_date() {
            return certificate_document_date;
        }

        /**
         * Устанавливает дату сертификата.
         *
         * @param certificate_document_date Дата сертификата.
         */
        public void setCertificate_document_date(String certificate_document_date) {
            this.certificate_document_date = certificate_document_date;
        }

        /**
         * Получает номер сертификата.
         *
         * @return Номер сертификата.
         */
        public String getCertificate_document_number() {
            return certificate_document_number;
        }

        /**
         * Устанавливает номер сертификата.
         *
         * @param certificate_document_number Номер сертификата.
         */
        public void setCertificate_document_number(String certificate_document_number) {
            this.certificate_document_number = certificate_document_number;
        }

        /**
         * Получает ИНН владельца.
         *
         * @return ИНН владельца.
         */
        public String getOwner_inn() {
            return owner_inn;
        }

        /**
         * Устанавливает ИНН владельца.
         *
         * @param owner_inn ИНН владельца.
         */
        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        /**
         * Получает ИНН производителя.
         *
         * @return ИНН производителя.
         */
        public String getProducer_inn() {
            return producer_inn;
        }

        /**
         * Устанавливает ИНН производителя.
         *
         * @param producer_inn ИНН производителя.
         */
        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        /**
         * Получает дату производства.
         *
         * @return Дата производства.
         */
        public String getProduction_date() {
            return production_date;
        }

        /**
         * Устанавливает дату производства.
         *
         * @param production_date Дата производства.
         */
        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        /**
         * Получает код ТН ВЭД.
         *
         * @return Код ТН ВЭД.
         */
        public String getTnved_code() {
            return tnved_code;
        }

        /**
         * Устанавливает код ТН ВЭД.
         *
         * @param tnved_code Код ТН ВЭД.
         */
        public void setTnved_code(String tnved_code) {
            this.tnved_code = tnved_code;
        }

        /**
         * Получает код УИТ.
         *
         * @return Код УИТ.
         */
        public String getUit_code() {
            return uit_code;
        }

        /**
         * Устанавливает код УИТ.
         *
         * @param uit_code Код УИТ.
         */
        public void setUit_code(String uit_code) {
            this.uit_code = uit_code;
        }

        /**
         * Получает код УИТУ.
         *
         * @return Код УИТУ.
         */
        public String getUitu_code() {
            return uitu_code;
        }

        /**
         * Устанавливает код УИТУ.
         *
         * @param uitu_code Код УИТУ.
         */
        public void setUitu_code(String uitu_code) {
            this.uitu_code = uitu_code;
        }
    }
}
