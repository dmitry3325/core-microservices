# CoreMicroservices Backend

## Overview

Overview
Core Microservices provides a set of essential services to support startup applications 
by offering modular and scalable services, to help companies save time and resources, allowing them to focus on their unique business logic.


The project will include services like:

- User Management - managing users, roles, and permissions.
- Notification service - sending emails, SMS, and push notifications, simple chat service.
- Document Management Service - managing documents, files, and attachments.
- Localization Service - managing translations for multiple languages.
- Template Service - managing email templates, SMS templates, and document templates in HTML format.
- Questionnaire Service - managing questionnaires, surveys, and forms.
- Product Management Service - managing products, categories, and orders.
- Payment Service - managing payments, invoices, and subscriptions.
- Analytics Service - managing analytics, reports, and dashboards.

More services will be added in the future to provide a complete set of services for a startup application.
You can choose what services you need and integrate them into your application with further customization.

PLS NOTE: This project is still under development and not ready for production use.

Contact me if you want to contribute or have any suggestions. 

## Setup

Prerequisites
- Java 17+ (for Spring Boot compatibility)
- Maven
- PostgreSQL or your preferred database

## Project Structure

The project is organized into multiple modules:

- `user-ms`: Contains the user management service.
    - `user-api`: API definitions for user management.
    - `user-service`: The main service for user management.
- `notification-ms`: Contains the notification service.
    - `notification-api`: API definitions for notifications.
    - `notification-serice`: The main service for notification management.


## Configuration

each service has its own .env-example file that you can use to create your own .env file

### Database Configuration

The application uses PostgreSQL. Check configuration files in the `src/main/resources/db-config.yaml`.

## Building and Running the Application

### Build the Project

To build the project, navigate to the root directory and run:

```sh
mvn clean install
```

### Run the Application

To run the application, navigate to the `user-service` module and execute:

```sh
mvn spring-boot:run
```

## Testing

The project includes unit and integration tests. To run the tests, use the following command:

```sh
mvn test
```

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.