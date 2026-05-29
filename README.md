# Hackathon Rules
- Versions: Use Platform 25.2.0-beta1
- Choose your project: You can work on an app, fixes, migrations, new features, docs, addons, DS, or just reporting issues.
- Contribution options: If you don’t have any code to show, please add a README or screenshots demoing your work.
- Communication: Use Slack channel #hackathon-25-2 for discussions.
- Documentation: Refer to https://vaadin.com/docs/next/ for help and information.

# My Findings
- tried 'Quick Start Tutorial'->download project
  - Asked AI-prompt to help me setting up a view
  - magically switched to IDE and added 'HomeView' there
  - ... ? And now? I stare at the IDE, then stare at my running app. Ahh: found the very little note saying:'restart the server'. Would be helpful to put that more prominent
  - And the next statement wasn't exactly true:'The page will refresh automatically when the server is ready' didn't happen. I had to reload the browser page manually

- then tried to set up something to play with the Multi-Combobox and Signals.
  - Signals: got some misleading answers from AI then tried read in code and docs. Idea: let the text input be split as values for a ListDataProvider.
  - Found that I cannot set items to a ListDataProvider as 'backend' is final and used inside directly without using the getter method
  - Did not get the coupling of text input, signals, and items for the DataProvider connected
    - suddenly time was up