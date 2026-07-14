## 2024-07-14 - Evaluated FAB Accessibility
**Learning:** The main interaction button in this app is a FloatingActionButton (FAB) which previously had no contentDescription, meaning it would be completely unreadable by TalkBack and other Android screen readers.
**Action:** Always check icon-only buttons (like ImageButton and FloatingActionButton) in Android XML layouts for the android:contentDescription attribute, as it's a common accessibility oversight.
