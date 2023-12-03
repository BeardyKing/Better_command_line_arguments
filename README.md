# Better command line arguments *(CLion Plugin)*

![Build](https://github.com/BeardyKing/better_command_line_args/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

### Info:
Better command line arguments is a small plugin that manages CLion "program arguments" as a tree structures, This plugin is highly inspired by the Visual Studio plugin [SmartCommandlineArgs](https://github.com/MBulli/SmartCommandlineArgs)

### Warning:
This plugin will write raw command line text to `workspace.xml` which is used by CLion to manage Configurations, be sure to validate that your `.idea` folder is ignored from public repositories.

The `CLArgs.json` file saves your current CLArg tree, but doesn't doesn't store raw environment variables data, instead it stores the environment variable name i.e. `<OS>` this could potentially resolve to `Windows_NT` and can be viewed in the command line argument preview at the bottom of the plugin.

### Preview:
![res/clarg_tree_screenshot.png](example CLArg tree)

### Roadmap:
Better command line arguments in its current form are presented in a state of a minimal viable plugin. I've written this plugin to match a simple workflow that I use on a daily basis. This plugin doesn't match all the features the visual studio plugin has but maybe in the future, I have provided a list of various features that this plugin would benefit from having.

If there are any features you would like feel free to open a pull request.

- [ ] Rebindable shortcuts
- [ ] View CLion program argument macros in CL Vars tab
- [ ] Toggle hide environment variable preview
- [ ] Folder conditions ( *i.e. toggles based on config settings* )
- [ ] Undo/Redo last action

#### Shortcuts:

| keybind      | use                                                       |
| ------------ | --------------------------------------------------------- |
| "Insert"     | Add CLArg to current selected folder                      |
| "Delete"     | Remove all currently selected CLArgs                      |
| "Home"       | Add all currently selected CLArgs to new Folder           |
| "Space"      | Toggle all currently selected CLArgs on/off               |
| "Alt + Up"   | Move all currently selected CLArgs up in current folder   | 
| "Alt + Down" | Move all currently selected CLArgs down in current folder |
| "Ctrl + R"   | Manually parse CLArgs and save to workspace.xml           |
| "Ctrl + V"   | Add CLArg with text from the clipboard in current folder  |


## Installation

- Using the IDE built-in plugin system:
 
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Better command line arguments"</kbd> >
  <kbd>Install</kbd>
 
- Manually:

  Download the [latest release](https://github.com/BeardyKing/better_command_line_args/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
