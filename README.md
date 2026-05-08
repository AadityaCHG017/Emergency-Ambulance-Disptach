# Emergency Ambulance Dispatch System 🚑

An AI-powered emergency ambulance dispatch system built with JavaFX that streamlines emergency response coordination and optimizes ambulance allocation for faster patient care.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Usage](#usage)
- [AI-Powered Features](#ai-powered-features)
- [Contributing](#contributing)
- [Contact](#contact)

## 🎯 Overview

The Emergency Ambulance Dispatch System is a comprehensive solution designed to revolutionize emergency medical services (EMS) coordination. By leveraging artificial intelligence and modern Java technologies, this system ensures rapid response times, intelligent ambulance allocation, and efficient management of emergency requests.

### Key Objectives

- **Minimize Response Time**: Optimize ambulance routing and assignment to reduce emergency response times
- **Intelligent Dispatch**: Use AI algorithms to match the right ambulance to each emergency
- **Real-time Tracking**: Monitor ambulance locations and availability in real-time
- **Data-Driven Decisions**: Analyze historical data to improve future dispatch operations
- **User-Friendly Interface**: Provide intuitive interfaces for dispatchers, drivers, and administrators

## ✨ Features

### Core Functionality

- **Emergency Request Management**
  - Accept and process emergency calls
  - Capture critical patient information
  - Prioritize requests based on severity

- **Intelligent Ambulance Dispatch**
  - AI-powered ambulance selection
  - Distance-based optimization
  - Real-time availability tracking
  - Automatic reassignment on timeout/rejection

- **Fleet Management**
  - Track all ambulances in the system
  - Monitor vehicle status (available, dispatched, in-service, maintenance)
  - Manage driver assignments
  - Vehicle maintenance scheduling

- **Hospital Integration**
  - Hospital availability tracking
  - Bed capacity management
  - Nearest hospital identification
  - Specialist availability checking

- **User Management**
  - Role-based access control (Admin, Dispatcher, Driver)
  - Secure authentication and authorization
  - User profile management
  - Activity logging and audit trails

- **Analytics & Reporting**
  - Response time analytics
  - Performance metrics
  - Historical data analysis
  - Custom report generation

## 🛠 Technology Stack

### Frontend
- **JavaFX**: Modern desktop UI framework
- **FXML**: Declarative UI design
- **CSS**: Custom styling and themes

### Backend
- **Java**: Core application logic
- **Maven**: Dependency management and build automation

### Data Storage
- **File-based Storage**: Local data persistence in `user_data/` directory
- Structured data formats for users, ambulances, and emergency records

### AI/ML Components
- Custom algorithms for:
  - Ambulance assignment optimization
  - Route calculation
  - Demand prediction
  - Priority scoring

## 🏗 System Architecture

```
Emergency-Ambulance-Dispatch/
│
├── src/main/
│   ├── java/              # Java source files
│   │   ├── controllers/   # JavaFX controllers
│   │   ├── models/        # Data models
│   │   ├── services/      # Business logic
│   │   ├── utils/         # Utility classes
│   │   └── ai/            # AI/ML algorithms
│   │
│   └── resources/         # Application resources
│       ├── fxml/          # FXML layout files
│       ├── css/           # Stylesheets
│       └── images/        # Icons and images
│
├── user_data/             # Application data storage
│   ├── users.dat          # User accounts
│   ├── ambulances.dat     # Ambulance records
│   └── emergencies.dat    # Emergency history
│
├── target/                # Compiled application
├── pom.xml               # Maven configuration
└── README.md             # This file
```

## 🚀 Getting Started

### Prerequisites

Before running the application, ensure you have the following installed:

- **Java Development Kit (JDK) 11 or higher**
  - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
  - Verify installation: `java -version`

- **Apache Maven 3.6 or higher**
  - Download from [Maven Official Site](https://maven.apache.org/download.cgi)
  - Verify installation: `mvn -version`

- **Git** (for cloning the repository)
  - Download from [Git Official Site](https://git-scm.com/)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/AadityaCHG017/Emergency-Ambulance-Disptach.git
   cd Emergency-Ambulance-Disptach
   ```

2. **Install dependencies**
   ```bash
   mvn clean install
   ```

3. **Build the project**
   ```bash
   mvn package
   ```

### Running the Application

#### Using Maven
```bash
mvn javafx:run
```

#### Using Compiled JAR
```bash
java -jar target/emergency-ambulance-dispatch-1.0.jar
```

#### Default Login Credentials
- **Admin**
  - Username: `admin`
  - Password: `admin123`

- **Dispatcher**
  - Username: `dispatcher`
  - Password: `dispatch123`

- **Driver**
  - Username: `driver`
  - Password: `driver123`

> ⚠️ **Important**: Change default passwords after first login for security purposes.

## 📁 Project Structure

### Key Modules

#### Controllers
Handles UI interactions and user input processing:
- `LoginController`: User authentication
- `DashboardController`: Main dashboard interface
- `DispatchController`: Emergency dispatch operations
- `AmbulanceController`: Fleet management
- `ReportController`: Analytics and reporting

#### Models
Data structures representing system entities:
- `User`: User account information
- `Ambulance`: Ambulance details and status
- `Emergency`: Emergency request data
- `Hospital`: Hospital information
- `Route`: Route and navigation data

#### Services
Business logic and core functionality:
- `AuthenticationService`: User login/logout
- `DispatchService`: Ambulance assignment logic
- `LocationService`: GPS and mapping functionality
- `NotificationService`: Alert and notification management
- `AIService`: Machine learning algorithms

#### Utils
Helper classes and utilities:
- `DatabaseUtil`: Data persistence operations
- `ValidationUtil`: Input validation
- `DateTimeUtil`: Date/time operations
- `DistanceCalculator`: Haversine distance calculation

## 📖 Usage

### For Dispatchers

1. **Receiving Emergency Calls**
   - Open the Dispatch Dashboard
   - Click "New Emergency Request"
   - Enter patient information and location
   - Select priority level
   - Submit request

2. **Assigning Ambulances**
   - View available ambulances on the map
   - System recommends optimal ambulance
   - Review and confirm assignment
   - Monitor ambulance response

3. **Managing Active Emergencies**
   - Track all active emergencies in real-time
   - Update emergency status
   - Communicate with drivers
   - Complete emergency records

### For Drivers

1. **Accepting Assignments**
   - Receive emergency notifications
   - View emergency details and location
   - Accept or reject assignment
   - Navigate to emergency location

2. **Updating Status**
   - Update availability status
   - Report current location
   - Update emergency progress
   - Complete trip reports

### For Administrators

1. **Fleet Management**
   - Add/remove ambulances
   - Assign drivers to vehicles
   - Schedule maintenance
   - Monitor vehicle status

2. **User Management**
   - Create user accounts
   - Assign roles and permissions
   - Review user activity
   - Manage access control

3. **System Configuration**
   - Configure system settings
   - Set operational parameters
   - Manage hospital database
   - Configure AI algorithms

## 🤖 AI-Powered Features

### Intelligent Ambulance Assignment

The system uses sophisticated algorithms to select the optimal ambulance for each emergency:

```java
// Pseudocode for ambulance assignment
function selectBestAmbulance(emergency):
    availableAmbulances = getAvailableAmbulances()
    
    for ambulance in availableAmbulances:
        score = calculateScore(
            distance = calculateDistance(ambulance.location, emergency.location),
            ambulanceType = matchAmbulanceType(ambulance, emergency.severity),
            trafficConditions = getTrafficData(ambulance.location, emergency.location),
            driverExperience = ambulance.driver.experience
        )
        ambulance.assignmentScore = score
    
    return ambulanceWithHighestScore(availableAmbulances)
```

### Distance Calculation

Uses the Haversine formula for accurate distance calculation:

```
a = sin²(Δφ/2) + cos φ₁ · cos φ₂ · sin²(Δλ/2)
d = 2R · arctan2(√a, √(1−a))
```

Where:
- φ = latitude
- λ = longitude
- R = Earth's radius (≈6,371 km)

### Predictive Analytics

- **Demand Forecasting**: Predicts emergency call volumes based on historical data
- **Hot Spot Identification**: Identifies areas with high emergency frequency
- **Optimal Positioning**: Suggests strategic ambulance positioning
- **Response Time Optimization**: Continuously improves dispatch efficiency

## 🤝 Contributing

We welcome contributions to improve the Emergency Ambulance Dispatch System!

### How to Contribute

1. **Fork the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/Emergency-Ambulance-Disptach.git
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/YourFeatureName
   ```

3. **Make your changes**
   - Follow Java coding conventions
   - Add appropriate comments
   - Update documentation as needed

4. **Commit your changes**
   ```bash
   git commit -m "Add: Brief description of changes"
   ```

5. **Push to your branch**
   ```bash
   git push origin feature/YourFeatureName
   ```

6. **Create a Pull Request**
   - Describe your changes
   - Reference any related issues
   - Wait for review and feedback

### Coding Standards

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Keep methods focused and concise
- Write unit tests for new features

## 📧 Contact

**Aaditya CHG**

- GitHub: [@AadityaCHG017](https://github.com/AadityaCHG017)
- Repository: [Emergency-Ambulance-Disptach](https://github.com/AadityaCHG017/Emergency-Ambulance-Disptach)


## 🔮 Future Enhancements

- [ ] Real-time GPS tracking integration
- [ ] Mobile app for drivers and patients
- [ ] Integration with hospital management systems
- [ ] Advanced machine learning models
- [ ] Multi-language support
- [ ] Cloud deployment option
- [ ] WebSocket-based real-time updates
- [ ] Integration with traffic APIs
- [ ] Automated testing suite
- [ ] Docker containerization
