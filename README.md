# Networking Class Project

## Overview

This repository contains code from a networking class project focused on simulating network communication and packet transfer. The project implements a custom network simulator, message transmission system, and packet handling logic to explore concepts such as packet loss, corruption, and retransmissions.

## Key Components

### 1. **Project.java**
This is the entry point of the application. It initializes the network simulator with parameters such as:
- Number of messages to simulate
- Packet loss and corruption probabilities
- Average message delay
- Retransmission timeout

### 2. **NetworkSimulator.java**
An abstract class that forms the backbone of the simulation. Key features include:
- Event-driven simulation of network activities
- Management of timers, packets, and messages
- Handling packet transmission, corruption, and loss
- Abstract methods for sender and receiver behavior (`aOutput`, `aInput`, `bInput`, etc.)

### 3. **Packet.java**
Represents a network packet with fields for:
- Sequence number
- Acknowledgment number
- Checksum
- Payload (message data)
Includes methods to create, validate, and manipulate packets.

### 4. **Message.java**
Defines the structure of a message, with validation to ensure the size does not exceed the maximum allowable size (`MAXDATASIZE`).

### 5. **OSIRandom.java**
A custom random number generator used for simulating:
- Packet loss
- Corruption
- Timing variations

### 6. **EventList.java**
An interface for managing the list of events in the simulation. Events include message arrivals, timer expirations, and packet transmissions.

## Features

- **Event-Driven Simulation**: The simulator processes events sequentially, allowing for a detailed emulation of network behavior.
- **Packet Handling**: Simulates realistic scenarios including:
  - Packet loss
  - Packet corruption
  - Retransmissions
- **Customizable Parameters**: Users can configure the simulation through input parameters, such as loss probability and window size.
- **Object-Oriented Design**: Encapsulation of network entities like packets, messages, and events for modularity and reusability.

## Potential Applications
- Understanding the principles of reliable data transfer in networks
- Experimenting with different configurations for network reliability
- Exploring the impact of network issues like packet loss and corruption
