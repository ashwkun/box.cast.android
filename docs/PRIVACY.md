# What we track and why

> **boxlore isn’t meant for monetary gain. It’s a few friends finishing a childhood dream: watching code turn into magic on screens.**
>
> *Never sold · Never for ads · No ads in boxlore · Usage only*

Whatever we collect will never be sold, never used to push ads, and never used for the other creepy things people do with data. It is only to understand how many people use the app, how they use it, and which features work or don't.

---

## What this looks like, and what it isn’t

Analytics can suggest a rough sketch (rough geographic region, device model, general podcast taste), but never a precise identity profile, and never age or gender. We don’t ask for your name or email, and there is no account; your subscriptions and library stay on your device.

The only personal details that show up are ones you type into search or AI onboarding. Please don't type sensitive personal info into search boxes.

And again: data is never sold, and never used for ads. **boxlore will never have ads.**

---

## The 5 Things We Collect

### 1. App use
* **What's collected:** Screen opens, feature taps, settings changes, and rough time spent. Things like home carousel swipes, explore searches, library visits, settings opens, and mini-player taps.
* **Why:** So we can see how many people are actually using boxlore, which parts feel alive, and which ones flop, so we can make better product calls. And honestly, so we can watch the charts and feel happy that people are using something we built.
* **Real example:** Earlier builds had a Radio feature. Usage data showed almost nobody used it, so we removed it instead of keeping dead weight in the app.

### 2. Search and onboarding text
* **What's collected:** Search queries and what the app returns, plus AI onboarding chat text.
* **Why:** Podcast Index and Apple's APIs lean hard on exact-word matching. Search quality is one of the biggest pain points we’re trying to fix, so seeing real queries and responses is critical. Same for AI onboarding: we need to know whether prompts are understood and answers stay relevant.
* **Real example:** A lot of people typed real podcast names into onboarding and treated the AI like a search box. The model kept asking generic taste questions instead of helping. So we added a layer that checks if the text is basically a show name, validates it against the chat context, and offers "use search instead" with that title so you can subscribe immediately.

### 3. Listening activity
* **What's collected:** Podcast and episode details, playback progress, likes, subscriptions, downloads, and related listening signals.
* **Why:** Public charts aren't great for granular signals, and Apple charts don't provide the play-level signal we need. We use this to understand listening trends and build community charts that are native to boxlore.

### 4. App and device
* **What's collected:** App version, OS version, device manufacturer/model, local hour, and an anonymous analytics ID.
* **Why:** We don’t really need this for product decisions; PostHog tracks it by default, and we don’t currently have a clean way to turn that part off.

### 5. Crashes and errors
* **What's collected:** Technical stack traces, error codes, and crash reports.
* **Why:** Pretty obvious: if the app breaks on a specific Android version or car head unit, we need to know so we can fix it.

---

## On-device recommendations (What stays on your phone)

Your learned taste profile and ranking model stay on your device. They are not uploaded. JSON backups you create include this state so an imported install can continue with the same learning without any server profile.

---

## Full Event Glossary

For complete transparency, our exact event schema, property definitions, and emission rules are documented in:
* **[Analytics Event Glossary](ANALYTICS_EVENT_GLOSSARY.md)**
* **[Companion Event CSV](analytics/event_glossary.csv)**
