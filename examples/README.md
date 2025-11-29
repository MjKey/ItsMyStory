# Storykee Example Stories

This directory contains example stories to help you learn Storykee and get started creating your own interactive adventures.

## Directory Structure

```
examples/
├── tutorial/          # Beginner-friendly tutorial story
│   ├── main.skee      # Main script file
│   └── config.json    # Story configuration
├── advanced/          # Advanced features demonstration
│   ├── main.skee      # Complex story with all features
│   └── config.json    # Story configuration
├── showcase/          # Complete feature showcase (ALL features!)
│   ├── main.skee      # Demonstrates every mod capability
│   └── config.json    # Story configuration
└── template/          # Blank template for new stories
    ├── main.skee      # Template with comments
    └── config.json    # Configuration template
```

## Getting Started

### Installing an Example Story

1. Copy the desired example folder to your Minecraft installation:
   ```
   .minecraft/itsmystory/scripts/
   ```

2. Start Minecraft with the It's My Story mod installed

3. The story will automatically load when you join a world

### Tutorial Story: "First Steps"

**Difficulty:** Beginner  
**Estimated Time:** 5-10 minutes

This tutorial teaches the basics of Storykee:
- Variables and basic operations
- NPC creation and interaction
- Simple dialogues with choices
- Basic quest tracking
- Event handling

Perfect for first-time story creators!

### Advanced Story: "The Lost Artifact"

**Difficulty:** Advanced  
**Estimated Time:** 2-4 hours

This comprehensive example demonstrates:
- Complex quest chains with prerequisites
- Multi-phase boss encounters
- Cinematic cutscenes with camera control
- World modifications (blocks, structures)
- Particle effects and audio
- Timer-based events
- Player variable persistence
- Java section integration for advanced features

Study this example to learn how to create epic adventures!

### Showcase Story: "Storykee Feature Showcase"

**Difficulty:** Reference  
**Purpose:** Complete demonstration of ALL mod features

This showcase demonstrates EVERY capability of the mod:
- **NPC System**: NPCs spawn near the player (not at fixed coordinates!)
- **Dialogues**: Branching conversations with conditions
- **Quests**: Objectives, rewards, and progress tracking
- **Bosses**: Multi-phase encounters with abilities
- **Cutscenes**: Cinematic camera control
- **HUD/GUI**: Custom interface elements
- **Audio**: Sounds and music
- **Timers**: Delayed and repeating actions
- **World Modification**: Block placement and particles
- **Regions**: Area detection and triggers
- **Holograms**: Floating text displays
- **Variables**: Player and global persistence
- **Java Sections**: Advanced embedded Java code
- **Events**: All event types demonstrated

Use this as a reference when building your own stories!

### Template

Use the template as a starting point for your own stories. It includes:
- Commented code explaining each feature
- Placeholder content ready to customize
- Best practices and common patterns

## Quick Reference

### Basic Syntax

```storykee
// Variables
var myVariable = "value";
var number = 42;
var isActive = true;

// NPC Definition
npc characterName {
    name: "Display Name";
    skin: "skin_file";
    position: vec3(x, y, z);
}

// Dialogue
dialogue dialogueName {
    node start {
        text: "Hello!";
        choices: [
            { text: "Option 1", next: "node1" },
            { text: "Option 2", next: "node2" }
        ];
    }
}

// Quest
quest questName {
    title: "Quest Title";
    objectives: [
        { id: "obj1", type: "interact_npc", target: "npc", count: 1 }
    ];
    rewards: { experience: 100 };
}

// Event Handler
on playerJoin {
    var player = event.player;
    // Handle event
}
```

### Common Functions

| Function | Description |
|----------|-------------|
| `print(message)` | Log message to console |
| `spawnNPC(name, x, y, z, props)` | Create an NPC |
| `despawnNPC(name)` | Remove an NPC |
| `showDialogue(player, dialogueName)` | Start a dialogue |
| `startQuest(player, questName)` | Begin a quest |
| `completeQuest(player, questName)` | Complete a quest |
| `giveItem(player, itemId, count)` | Give items to player |
| `teleport(player, x, y, z)` | Teleport player |
| `playSound(player, soundId, volume, pitch)` | Play a sound |
| `playMusic(player, musicId, loop, fadeIn)` | Play background music |
| `scheduleDelayed(ms, callback)` | Execute after delay |

### Event Types

| Event | Properties |
|-------|------------|
| `playerJoin` | `event.player` |
| `playerLeave` | `event.player` |
| `npcInteract` | `event.player`, `event.npc` |
| `regionEnter` | `event.player`, `event.region` |
| `regionExit` | `event.player`, `event.region` |
| `questComplete` | `event.player`, `event.quest` |
| `entityDeath` | `event.player`, `event.entity` |
| `blockBreak` | `event.player`, `event.block`, `event.position` |
| `blockPlace` | `event.player`, `event.block`, `event.position` |

## Tips for Story Creators

1. **Start Simple**: Begin with the tutorial example and gradually add complexity

2. **Use Hot Reload**: Enable `hotReload: true` in config.json to see changes without restarting

3. **Test Often**: Use `debugMode: true` during development for detailed logging

4. **Organize Code**: Split large stories into multiple .skee files (npcs.skee, quests.skee, etc.)

5. **Comment Your Code**: Future you will thank present you

6. **Back Up Your Work**: Use version control or regular backups

## Need Help?

- Check the full documentation in `docs/`
- Review the language reference for complete syntax
- Look at the advanced example for complex patterns

Happy storytelling! 🎮📖
