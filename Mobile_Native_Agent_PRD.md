# Mobile-Native Conversational Agent
### Technical Specification & Product Requirements Document

> **"Talk normally -> Agent understands -> User approves -> Agent does it."**

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Core Concept](#2-core-concept)
3. [Interaction Model](#3-interaction-model)
4. [Natural Language Understanding Requirements](#4-natural-language-understanding-requirements)
5. [MVP Workflows](#5-mvp-workflows)
6. [System Architecture](#6-system-architecture)
7. [Technical Stack](#7-technical-stack)
8. [Agent Pipeline](#8-agent-pipeline)
9. [WhatsApp Message Reading](#9-whatsapp-message-reading)
10. [Cross-Application Execution Layer](#10-cross-application-execution-layer)
11. [Ambiguity & Clarification Handling](#11-ambiguity--clarification-handling)
12. [Confirmation Protocol](#12-confirmation-protocol)
13. [MVP Scope & Constraints](#13-mvp-scope--constraints)
14. [Target Users](#14-target-users)

---

## 1. Problem Statement

Smartphones are the primary interface through which people communicate, consume information, perform transactions, and interact with digital services. Yet most mobile interactions remain **app-centric and manually orchestrated**.

A conversation on WhatsApp frequently implies an action in another application. The user must:

1. Read and understand the conversation
2. Identify what needs to be done
3. Remember the relevant context
4. Switch to the appropriate application
5. Manually perform the required steps
6. Switch back to the original conversation

This produces unnecessary friction, context switching, and cognitive overhead.

### Illustrative Examples

| Message Received | What the user actually has to do |
|---|---|
| *"bhai 2 blue Lays aur ek Coke daal de"* | Open shopping app -> search products -> select variants -> add to cart |
| *"client ke liye quotation bana ke bhej de, 12 units 4500 each"* | Open ChatGPT -> write prompt -> generate doc -> download PDF -> open WhatsApp -> find chat -> send |
| *"apna number aur location bhej"* | Find contact info -> open maps -> copy location -> switch to WhatsApp -> send both |

> **The pattern:** A natural conversation contains an implicit task, and completing that task requires actions across multiple applications or phone capabilities.

---

## 2. Core Concept

A **mobile-native conversational agent** that:

- Reads ordinary WhatsApp conversations
- Identifies when a conversation implies an actionable task
- Interprets the intent -- even from informal, incomplete, or mixed-language messages
- Presents its interpretation to the user for confirmation
- Executes the required cross-application workflow only after approval
- Reports back when done

The agent does not require the user to issue structured commands. It works with the way people naturally communicate.

```
WhatsApp Conversation  ->  Agent Understanding  ->  User Confirmation  ->  Cross-App Execution
```

---

## 3. Interaction Model

The fundamental interaction loop is:

```
Conversation -> Understanding -> Confirmation -> Execution -> Completion Report
```

### Step-by-step Flow

```
+----------------------------------------------------------+
|  STEP 1  | User communicates naturally in WhatsApp       |
+----------------------------------------------------------+
|  STEP 2  | Agent reads conversation and infers task      |
+----------------------------------------------------------+
|  STEP 3  | Agent explains what it understood             |
+----------------------------------------------------------+
|  STEP 4  | Agent proposes the exact actions it will take |
+----------------------------------------------------------+
|  STEP 5  | User confirms: YES or NO                      |
+----------------------------------------------------------+
|  STEP 6  | If YES -> Agent executes cross-app workflow   |
|           | If NO  -> Workflow terminates, no action taken|
+----------------------------------------------------------+
|  STEP 7  | Agent reports completion to the user          |
+----------------------------------------------------------+
```

### Confirmation UI Example

```
+----------------------------------------------+
|  I understood this as:                       |
|  * 2 x Lays Classic Salted                   |
|  * 1 x Coca-Cola                             |
|  * Add them to your cart on Blinkit          |
|                                              |
|  Should I proceed?                           |
|                                              |
|        [ Yes ]         [ No ]                |
+----------------------------------------------+
```

---

## 4. Natural Language Understanding Requirements

The agent must work with the full range of how people actually communicate on messaging apps.

### Informal language
```
"bhai ye kar de"
"haan usko bhej"
"woh wala order karde"
```

### Short and implicit messages
```
"send him that"
"same one"
"haan"
"yeh"
```

### Incomplete references
```
"woh PDF"     ->  resolve which PDF from context
"kal wala"    ->  resolve from conversation history
"blue wala"   ->  resolve product variant from context
"usko"        ->  resolve recipient from conversation
```

### Mixed language (Hinglish)
```
"bhai ye wala order krde"
"client ko PDF bana ke bhej"
"apna location send karde"
```

### Multi-turn, fragmented intent

A single task may be expressed across multiple messages:

```
"bhai quotation bana de"
"ABC Industries ke liye"
"12 units"
"4500 each"
"PDF me bhejna"
```

The agent must aggregate these messages into a single coherent task rather than treating each as an independent instruction.

---

## 5. MVP Workflows

### Workflow 1 -- Product Ordering / Cart Preparation

**Trigger pattern:** User receives a message requesting products be ordered or added to a cart.

**Example input:**
> *"bhai 2 blue lays aur ek coke daal de"*

**Agent interpretation:**
```
I understood this as:
  * 2 x Lays Classic Salted (Blue)
  * 1 x Coca-Cola (Regular)
  * Add to cart on Blinkit

Should I proceed?   [ Yes ]  [ No ]
```

**Execution flow:**
```
WhatsApp
  |   parse message + resolve products
  v
Shopping Application (e.g. Blinkit / Zepto / Swiggy Instamart)
  |   search each product
  |   select correct variant
  v   add to cart
Cart Ready
  |
  v
Agent reports: "Done. Added 2 Lays (Blue) and 1 Coke to your cart."
```

> **Important:** The agent stops **before checkout**. It does not complete the purchase autonomously.

**Ambiguity handling:**
- *"blue wala"* -> resolve variant from conversation or ask
- *"same one"* -> reference last ordered item or ask
- Multiple matching products -> present options to user

---

### Workflow 2 -- AI-Assisted Document Creation & Sharing

**Trigger pattern:** User is asked to create a document (quotation, invoice, summary, etc.) and send it back through WhatsApp.

**Example input:**
```
"bhai client ke liye quotation bana ke bhej"
"ABC Industries ke naam pe"
"12 units"
"4500 each"
```

**Agent interpretation:**
```
I understood this as:
  Create a quotation for ABC Industries
  -> 12 units x Rs.4,500 each = Rs.54,000
  -> I'll open ChatGPT, prompt it to generate the PDF
  -> Download the result
  -> Send it back to this chat

Should I proceed?   [ Yes ]  [ No ]
```

**Execution flow:**
```
WhatsApp
  |   extract: document type, client name, quantity, price, format, recipient
  v
Agent constructs a detailed prompt
  |   e.g. "Generate a professional quotation PDF for ABC Industries,
  |          12 units at Rs.4500 each, total Rs.54,000. Format as a clean PDF."
  v
Open ChatGPT app (or Claude app) via Android Intent / Accessibility
  |   navigate to new chat
  |   type / paste the constructed prompt
  |   submit prompt
  v   wait for GPT to generate and produce downloadable PDF
Download the generated PDF
  |   tap the download button inside ChatGPT
  v   file saved to device storage
Switch back to WhatsApp
  |   navigate to the originating chat
  |   attach the downloaded PDF
  v   send
Agent reports: "Done. Quotation PDF sent to ABC Industries."
```

> **Key distinction:** The agent does **not** call any LLM API directly. It operates ChatGPT or Claude as a real app on the phone -- opening it, typing the prompt, and downloading the output -- exactly as a human would, but autonomously.

**Information extracted from conversation:**

| Field | Example value |
|---|---|
| Document type | Quotation |
| Client name | ABC Industries |
| Quantity | 12 units |
| Unit price | Rs.4,500 |
| Total | Rs.54,000 |
| Output format | PDF |
| Recipient | Sender of the original message / specified chat |

---

### Workflow 3 -- Contact Information & Current Location Sharing

**Trigger pattern:** User is asked to share their number, contact card, or current location.

**Example input:**
> *"apna number aur location bhej"*

**Agent interpretation:**
```
I understood this as:
  -> Send your phone number / contact card
  -> Send your current GPS location
  -> To this WhatsApp chat

Should I proceed?   [ Yes ]  [ No ]
```

**Execution flow:**
```
WhatsApp
  |   identify: location requested
  v
Accessibility Service controls WhatsApp UI
  |   tap attachment icon (paperclip / +)
  v   wait for attachment popup menu
Tap "Location" item in attachment menu
  |   wait for WhatsApp location picker map & nearby places to render
  v
Tap "Send your current location" (Accurate to X meters)
  |   WhatsApp directly sends the location pin into the chat
  v
Agent reports: "Done. Sent your current location via WhatsApp."
```

> **UI Automation Advantage:** The agent does not need background GPS permissions or custom link generation; it controls WhatsApp's native location picker directly on screen like a human user.

---

## 6. System Architecture

```
+--------------------------------------------------------------------------+
|                           Mobile Device                                  |
|                                                                          |
|  +--------------+    +-----------------------------------------------+  |
|  |  WhatsApp    | -> |               Agent Core                      |  |
|  | (Trigger /   |    |                                               |  |
|  |  Source)     |    |  +-------------+   +---------------------+   |  |
|  +--------------+    |  |  NLU Layer  | ->|  Task Planner       |   |  |
|                       |  | (LLM-based) |   |  (Intent + Steps)   |   |  |
|  +--------------+    |  +-------------+   +---------+-----------+   |  |
|  | Shopping App | <- |                              |               |  |
|  | (Blinkit etc)|    |  +---------------------------v-----------+   |  |
|  +--------------+    |  |        Confirmation Layer             |   |  |
|                       |  |  (Present interpretation to user)    |   |  |
|  +--------------+    |  +---------------------------+-----------+   |  |
|  | ChatGPT /    | <- |                              |               |  |
|  | Claude app   |    |  +---------------------------v-----------+   |  |
|  +--------------+    |  |        Execution Engine               |   |  |
|                       |  |  (Cross-app via Accessibility         |   |  |
|  +--------------+    |  |   Service / Android Intents)          |   |  |
|  | Device APIs  | <- |  +---------------------------------------+   |  |
|  |(GPS/Contacts)|    +-----------------------------------------------+  |
|  +--------------+                                                        |
+--------------------------------------------------------------------------+
```

### Components

| Component | Responsibility |
|---|---|
| **Conversation Reader** | Opens WhatsApp, scrolls chat, reads message bubbles via Accessibility Service |
| **NLU Layer** | Interprets message intent using an LLM; handles informal language, Hinglish, and fragmented multi-message intent |
| **Context Manager** | Maintains conversation history, resolves references like "woh wala", "usko", "kal wala" |
| **Task Planner** | Converts understood intent into a structured action plan |
| **Confirmation Layer** | Renders the agent's interpretation and proposed actions to the user |
| **Execution Engine** | Orchestrates cross-application actions via Accessibility Service and Android Intents |
| **Completion Reporter** | Sends a natural language summary of completed actions back to the user |

---

## 7. Technical Stack

### LLM / NLU

| Component | Technology |
|---|---|
| Intent understanding | GPT-4o / Claude 3.5 / Gemini 1.5 Pro |
| Context window | Full conversation history passed as context |
| Prompt strategy | System prompt with intent schema + few-shot Hinglish examples |

### Mobile Execution Layer

| Capability | Approach |
|---|---|
| App automation (read + interact) | Android Accessibility Services |
| Deep linking | Android Intents for supported apps |
| WhatsApp read access | Accessibility Service UI scraping |
| WhatsApp send actions | Share Intent / Accessibility-based interaction |
| Location | Android Location Manager (GPS/Network) |
| Contacts | Android Contacts Content Provider |
| File system | Android Storage APIs (PDF save/retrieve) |

### Document Generation (Workflow 2)

| Step | Technology |
|---|---|
| Prompt construction | Agent builds a natural language prompt from extracted entities |
| AI interaction | Android Accessibility Service drives the ChatGPT or Claude app |
| PDF download | Accessibility tap on download button inside the AI app |
| Storage | File lands in device Downloads folder -> shared to WhatsApp via FileProvider |

### Agent Host Application

| Aspect | Choice |
|---|---|
| Platform | Android (primary) |
| Language | Kotlin |
| Architecture | MVVM + Use-case layer |
| LLM communication | REST API (streaming supported) |
| UI | Jetpack Compose |

---

## 8. Agent Pipeline

```
User opens agent and points it at a WhatsApp chat
        |
        v
+-------------------+
|  Conversation     |  <- See Section 9: WhatsApp Message Reading
|  Context Builder  |     opens WhatsApp, scrolls chat, reads
|                   |     message bubbles via Accessibility Service
+--------+----------+
         |
         v
+-------------------+
|   LLM Intent      |  <- Structured prompt asking:
|   Classifier      |     - Is there an actionable task?
|                   |     - What is the intent?
|                   |     - What are the entities?
|                   |     - Are references resolved?
+--------+----------+
         |
         +---- No clear intent -> no action / ask for clarification
         |
         v
+-------------------+
|   Entity          |  <- Extract:
|   Extractor       |     product names, quantities, variants
|                   |     document type, client, amount
|                   |     recipient, contact info, location flag
+--------+----------+
         |
         v
+-------------------+
|  Reference        |  <- Resolve:
|  Resolver         |     "usko" -> contact name from conversation
|                   |     "woh wala" -> last mentioned product/file
|                   |     "kal wala" -> recent document/order
+--------+----------+
         |
         +---- Unresolvable reference -> ask user to clarify
         |
         v
+-------------------+
|  Task Plan        |  <- Produce ordered list of actions
|  Generator        |     with app targets and parameters
+--------+----------+
         |
         v
+-------------------+
|  Confirmation     |  <- Render to user:
|  UI               |     "I understood X. Should I do Y?"
+--------+----------+
         |
         +---- User says NO -> terminate, no action
         |
         v
+-------------------+
|  Execution        |  <- Step-by-step cross-app actions
|  Engine           |
+--------+----------+
         |
         v
+-------------------+
|  Completion       |  <- Natural language summary
|  Reporter         |     sent back to user
+-------------------+
```

---

## 9. WhatsApp Message Reading

Before the agent can understand anything, it must first **read the WhatsApp conversation**. WhatsApp does not expose a public API, so the agent uses Android's **Accessibility Service** to open WhatsApp, navigate to the relevant chat, and read message bubbles directly from the screen's view hierarchy.

---

### How It Works -- Scroll-and-Read

The user selects a chat (or the agent is pointed at one). The Accessibility Service then reads the chat by scrolling through it programmatically.

```
User triggers agent on a WhatsApp chat
        |
        v
Accessibility Service opens WhatsApp
  (via Intent: com.whatsapp + specific chat deep link)
        |
        v
Navigates to the target chat
        |
        v
+--- Scroll & Read Loop -------------------------------------------+
|                                                                  |
|  1. Read all visible message nodes from view hierarchy           |
|  2. For each node: extract sender + text + timestamp             |
|  3. Scroll UP by one screen height                               |
|  4. Wait for UI to settle (~300ms)                               |
|  5. Repeat until N messages collected (e.g. 5-10)               |
|     or scroll/time limit is reached                              |
|                                                                  |
+------------------------------------------------------------------+
        |
        v
Return structured message list to agent pipeline
        |
        v
Minimize WhatsApp / return to agent UI
```

---

### What the Accessibility View Hierarchy Gives You

Each WhatsApp message bubble is a `View` node in the hierarchy. From it the agent reads:

| Field | Source |
|---|---|
| Message text | `node.text` or `node.contentDescription` |
| Sender name | Parent container label (group chats) |
| Message direction | Node position -- right = sent, left = received |
| Timestamp | Timestamp sub-node text |
| Media type | Node class name (image, audio, document, etc.) |

---

### Extracted Message Data Model

The scraper produces a structured list that is passed directly to the LLM as conversation context:

```json
[
  {
    "sender": "Rahul",
    "direction": "incoming",
    "text": "bhai client ke liye quotation bana ke bhej",
    "timestamp": "14:03",
    "type": "text"
  },
  {
    "sender": "Rahul",
    "direction": "incoming",
    "text": "ABC Industries ke naam pe, 12 units 4500 each",
    "timestamp": "14:04",
    "type": "text"
  },
  {
    "sender": "Me",
    "direction": "outgoing",
    "text": "haan kr deta hun",
    "timestamp": "14:05",
    "type": "text"
  }
]
```

Collecting the last **5-10 messages** is enough to resolve fragmented references like *"haan wahi wala"*, *"usko"*, or *"kal wala document"* in most real conversations.

---

### Privacy Note

Message text extracted by the agent is:
- Held **in-memory only** during the active task session
- Never written to persistent storage
- Only the **minimum relevant context** (last 5-10 messages) is sent to the LLM
- The Accessibility permission must be granted explicitly by the user

---

## 10. Cross-Application Execution Layer

The execution layer is the mechanism through which the agent performs actions across multiple mobile applications.

### Approach: Android Accessibility Services

Android's **Accessibility Service** API allows an application to:
- Read the UI state of any application on screen
- Simulate tap, swipe, and text-input gestures
- Navigate between applications

This enables the agent to interact with any installed app -- including WhatsApp and shopping apps -- without requiring those apps to expose an API.

```
Agent requests action
        |
        v
Accessibility Service
        |
        +-- Find UI element (e.g. "Search" field)
        +-- Simulate tap
        +-- Type product name
        +-- Wait for results
        +-- Tap correct result
        +-- Tap "Add to Cart"
        +-- Return control to Agent
```

### Approach: Android Intents (where available)

For applications that expose deep links or share intents:

```kotlin
// Share a file via WhatsApp
val intent = Intent(Intent.ACTION_SEND).apply {
    type = "application/pdf"
    putExtra(Intent.EXTRA_STREAM, fileUri)
    setPackage("com.whatsapp")
}
startActivity(intent)
```

### Approach: Full On-Device App Automation (Workflow 2)

For document generation, the agent controls the **ChatGPT or Claude mobile app** directly using Accessibility Services -- no API keys, no server, no SDK.

```
Agent constructs prompt text
        |
        v
Accessibility Service opens ChatGPT / Claude app
        |
        v
Navigates to new conversation
        |
        v
Types / pastes the constructed prompt
        |
        v
Waits for generation to complete
        |
        v
Taps "Download" or "Export as PDF" inside app
        |
        v
File saved to device Downloads folder
        |
        v
Accessibility Service switches to WhatsApp
        |
        v
Attaches file -> sends to originating chat
```

> This is the same cross-app UI automation approach used for the shopping workflow -- the agent drives real apps the way a human would, making it work with any AI app the user already has installed.

---

## 11. Ambiguity & Clarification Handling

The agent must distinguish between what it knows and what it is guessing.

### Resolvable from context
```
"usko bhej"      ->  last mentioned contact name in chat
"woh PDF"        ->  last sent/received document in conversation
"blue wala"      ->  Lays Blue (Classic Salted), from product list
```

### Requires user clarification
```
"woh wala order karde"  (multiple possible products)

+------------------------------------------------------+
|  I think you mean a Lays packet, but I found two:   |
|                                                      |
|  1. Lays Classic Salted (Blue)                       |
|  2. Lays Magic Masala (Red)                          |
|                                                      |
|  Which one did you mean?                             |
+------------------------------------------------------+
```

### Unresolvable -- agent asks before proceeding
```
"send him that file"   (no prior context for "him" or "that file")

+------------------------------------------------------+
|  I couldn't determine:                               |
|  * Who should I send it to?                          |
|  * Which file are you referring to?                  |
|                                                      |
|  Could you clarify?                                  |
+------------------------------------------------------+
```

### Clarification loop
```
Agent asks clarifying question
         |
User provides answer
         |
Agent incorporates answer into task plan
         |
Agent presents updated confirmation
         |
User confirms -> Execute
```

---

## 12. Confirmation Protocol

Confirmation is a **mandatory, non-skippable step** before any action is taken.

### Requirements for a valid confirmation message

| Requirement | Example |
|---|---|
| State what the agent understood | "I understood that you want to order 2 Lays and 1 Coke" |
| State what actions it will take | "I will open Blinkit, search for these products, and add them to your cart" |
| State what it will NOT do | "I will stop before checkout -- no purchase will be made" |
| Provide a clear YES/NO choice | `[ Yes, proceed ]`  `[ No, cancel ]` |

### Bad confirmation (not acceptable)
```
"Found a task. Execute?"
"Task detected. Proceed?"
"I'll handle it. OK?"
```

### Good confirmation (required)
```
I understood that you want me to:
   * Add 2 x Lays Classic Salted to the cart
   * Add 1 x Coca-Cola to the cart
   * On Blinkit
   * I will NOT proceed to checkout

   Should I do this?  [ Yes ]  [ No ]
```

---

## 13. MVP Scope & Constraints

### In scope

- [x] Reading WhatsApp conversation context via Accessibility Service UI scraping
- [x] Interpreting informal, Hinglish, and fragmented messages via LLM
- [x] Workflow 1: Product cart preparation (Blinkit / Zepto / Swiggy Instamart)
- [x] Workflow 2: On-device ChatGPT/Claude automation + PDF download + WhatsApp send
- [x] Workflow 3: Own contact info + GPS location -> WhatsApp send
- [x] Mandatory user confirmation before any action
- [x] Ambiguity detection and clarification requests
- [x] Completion reporting in natural language
- [x] Terminate workflow cleanly on user rejection

### Out of scope for MVP

- [ ] Autonomous purchase completion (stop before checkout is intentional)
- [ ] Background monitoring of all WhatsApp chats
- [ ] Non-WhatsApp messaging apps (Telegram, SMS)
- [ ] iOS support
- [ ] Multi-step financial transactions
- [ ] Voice input

### Constraints

| Constraint | Detail |
|---|---|
| **No autonomous purchase** | Agent stops at cart stage; user checks out manually |
| **Confirmation is mandatory** | No action taken without explicit user YES |
| **Rejection is final** | Agent does not retry after a NO |
| **Data privacy** | Conversation content sent to LLM API must be handled per privacy policy |
| **Accessibility permission** | Required for cross-app UI automation and WhatsApp reading; user must grant explicitly |

---

## 14. Target Users

| Segment | Description |
|---|---|
| Office-going professionals | Use WhatsApp heavily for work coordination |
| Knowledge workers | Frequently create and share documents |
| Employees in Tier-1 cities | High smartphone penetration, app-heavy lifestyles |
| Small business owners / freelancers | Send quotations, invoices, and coordinate orders via WhatsApp |
| General heavy smartphone users | Regularly switch between messaging, shopping, and productivity apps |

The agent is designed around **natural human communication**, not users who speak to AI assistants in structured commands.

---

## Summary

```
+------------------------------------------------------------------+
|                                                                  |
|   The Mobile-Native Conversational Agent                         |
|                                                                  |
|   Understands ordinary WhatsApp conversations                    |
|   Identifies implied tasks                                       |
|   Explains its interpretation                                    |
|   Waits for confirmation                                         |
|   Executes cross-application workflows                           |
|   Reports completion                                             |
|                                                                  |
|   Without requiring the user to issue a single                   |
|   structured command.                                            |
|                                                                  |
+------------------------------------------------------------------+
```

| Workflow | Source | Agent does | Target |
|---|---|---|---|
| **Commerce** | WhatsApp message | Understands products + quantities -> adds to cart | Shopping app |
| **Creation** | WhatsApp message | Builds prompt -> drives ChatGPT app -> downloads PDF -> sends | ChatGPT app -> WhatsApp |
| **Native** | WhatsApp message | Reads contacts + GPS -> shares info | Device APIs -> WhatsApp |

---

*Document version: 1.0 -- MVP Specification*
