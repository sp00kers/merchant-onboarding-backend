# MerchantOnboardingBackend

This project was done in Spring Tool Suite version 4.31.0. The project was coded with Java in Spring Boot environment.

## MySQL

MySQL 8.0 will be End of Life by April 2026. MySQL 8.4 is the LTS version.

```bash
docker pull mysql:8.4
```

Then, do the command below to ensure MySQL is running:

```bash
docker run -d --name merchant-onboarding -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8.4
```

To verify whether it is running, do this:

```bash
docker ps
```
Afterwards, open MySQL through the terminal:

```bash
docker exec -it merchant-onboarding mysql -uroot -pyourpassword
```

## Starting back-end server

To start the back-end server, simply run the application through Boot Dashboard.

## Login
You can login as three pre-defined user roles using these credentials

```bash
Username: admin, Password: password123
```
```bash
Username: officer1,  Password: password123
```
```bash
Username: manager1, Password: password123
```
