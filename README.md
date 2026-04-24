# COMP1786_ExpenseTracker_KhoaLe
# 📊 COMP1786 - Expense Tracker System

![Course](https://img.shields.io/badge/Course-COMP1786-blue) 
![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Windows-lightgrey)
![Database](https://img.shields.io/badge/Database-Firebase-FFCA28)

A comprehensive expense tracking and project management system developed for the **COMP1786** coursework. This repository adopts a monorepo structure, containing two distinct applications designed to serve different user roles within the ecosystem. Both applications share data synchronously through a centralized **Firebase** backend.

## 📁 Repository Structure

This repository contains two main projects:

### 1. 📱 AdminApp_Java
**Platform:** Android Native (Java/XML)  
**Role:** Administration & Management App  
**Key Features:**
- Comprehensive dashboard for managing projects and budgets.
- Add, update, and delete projects, Expenses.
- Monitor project statuses (e.g., *At Risk*, *Done*).
- Advanced filtering and list views (`ProjectListActivity`).
- Direct Firebase Realtime Database integration.

### 2. 🌐 UserApp_MAUI
**Platform:** .NET MAUI (C#/XAML)  
**Role:** End-User Application (Cross-Platform)  
**Key Features:**
- View assigned projects and project details (`ProjectDetailsPage`).
- Log new expenses against specific projects.
- Add, update, and delete Expenses only.
- Responsive, modern UI with MVVM architecture (`CommunityToolkit.Mvvm`).
- Pull-to-refresh and real-time updates.

## 🛠️ Tech Stack & Tools

- **Backend:** Firebase (Authentication, Realtime Database)
- **Admin App:** Android Studio, Java, XML, Gradle
- **User App:** Visual Studio / JetBrains Rider, .NET 8 MAUI, C#, XAML

## 🚀 Getting Started

### Prerequisites
- A configured **Firebase** project with `google-services.json` (for Android) and `google-services-desktop.json` (for MAUI).
- **For Admin App:** Android Studio (latest version).
- **For User App:** Visual Studio 2022 (with .NET MAUI workload) or JetBrains Rider.

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/khoadz-daddy/COMP1786_ExpenseTracker_KhoaLe.git
