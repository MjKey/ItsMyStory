import * as vscode from 'vscode';

// Storykee built-in functions with documentation
const BUILTIN_FUNCTIONS: { [key: string]: { signature: string; description: string; params: string[] } } = {
    // Player functions
    'sendMessage': {
        signature: 'sendMessage(player, message)',
        description: 'Send a chat message to a player',
        params: ['player: Player', 'message: String']
    },
    'giveItem': {
        signature: 'giveItem(player, itemId, count)',
        description: 'Give items to a player',
        params: ['player: Player', 'itemId: String (e.g., "minecraft:diamond")', 'count: Number']
    },
    'takeItem': {
        signature: 'takeItem(player, itemId, count)',
        description: 'Remove items from a player\'s inventory',
        params: ['player: Player', 'itemId: String', 'count: Number']
    },
    'teleportPlayer': {
        signature: 'teleportPlayer(player, position)',
        description: 'Teleport a player to a position',
        params: ['player: Player', 'position: vec3']
    },
    'getPlayerVar': {
        signature: 'getPlayerVar(player, varName)',
        description: 'Get a persistent player variable',
        params: ['player: Player', 'varName: String']
    },
    'setPlayerVar': {
        signature: 'setPlayerVar(player, varName, value)',
        description: 'Set a persistent player variable',
        params: ['player: Player', 'varName: String', 'value: Any']
    },
    'addEffect': {
        signature: 'addEffect(player, effectId, duration, amplifier)',
        description: 'Add a potion effect to a player',
        params: ['player: Player', 'effectId: String', 'duration: Number (ticks)', 'amplifier: Number']
    },
    'removeEffect': {
        signature: 'removeEffect(player, effectId)',
        description: 'Remove a potion effect from a player',
        params: ['player: Player', 'effectId: String']
    },

    // Dialogue functions
    'showDialogue': {
        signature: 'showDialogue(player, dialogueId)',
        description: 'Start a dialogue with a player',
        params: ['player: Player', 'dialogueId: String']
    },

    // Quest functions
    'startQuest': {
        signature: 'startQuest(player, questId)',
        description: 'Start a quest for a player',
        params: ['player: Player', 'questId: String']
    },
    'completeQuest': {
        signature: 'completeQuest(player, questId)',
        description: 'Complete a quest for a player',
        params: ['player: Player', 'questId: String']
    },
    'updateObjective': {
        signature: 'updateObjective(player, questId, objectiveId, progress)',
        description: 'Update quest objective progress',
        params: ['player: Player', 'questId: String', 'objectiveId: String', 'progress: Number']
    },
    'hasActiveQuest': {
        signature: 'hasActiveQuest(player, questId)',
        description: 'Check if player has an active quest',
        params: ['player: Player', 'questId: String']
    },

    // Boss functions
    'spawnBoss': {
        signature: 'spawnBoss(bossId, config)',
        description: 'Spawn a boss entity with phases',
        params: ['bossId: String', 'config: Object { name, entityType, health, damage, position, phases }']
    },
    'showBossBar': {
        signature: 'showBossBar(player, bossId, name, color)',
        description: 'Show a boss bar to a player',
        params: ['player: Player', 'bossId: String', 'name: String', 'color: "red"|"green"|"blue"|"yellow"|"purple"|"white"']
    },
    'hideBossBar': {
        signature: 'hideBossBar(player, bossId)',
        description: 'Hide a boss bar from a player',
        params: ['player: Player', 'bossId: String']
    },

    // Audio functions
    'playSound': {
        signature: 'playSound(player, soundId, volume, pitch)',
        description: 'Play a sound for a player',
        params: ['player: Player', 'soundId: String', 'volume: Number (0.0-1.0)', 'pitch: Number (0.5-2.0)']
    },
    'playMusic': {
        signature: 'playMusic(player, trackId, loop, fadeIn)',
        description: 'Play background music for a player',
        params: ['player: Player', 'trackId: String', 'loop: Boolean', 'fadeIn: Number (seconds)']
    },
    'stopMusic': {
        signature: 'stopMusic(player, fadeOut)',
        description: 'Stop music for a player',
        params: ['player: Player', 'fadeOut: Number (seconds)']
    },

    // Visual effects
    'spawnParticles': {
        signature: 'spawnParticles(particleId, position, count, spread)',
        description: 'Spawn particles at a position',
        params: ['particleId: String', 'position: vec3', 'count: Number', 'spread: Number']
    },
    'showTitle': {
        signature: 'showTitle(title, subtitle, fadeIn, stay, fadeOut)',
        description: 'Show a title on screen',
        params: ['title: String', 'subtitle: String', 'fadeIn: Number (ticks)', 'stay: Number (ticks)', 'fadeOut: Number (ticks)']
    },
    'showActionBar': {
        signature: 'showActionBar(player, message)',
        description: 'Show a message in the action bar',
        params: ['player: Player', 'message: String']
    },
    'shakeScreen': {
        signature: 'shakeScreen(player, intensity, duration)',
        description: 'Shake the player\'s screen',
        params: ['player: Player', 'intensity: Number', 'duration: Number (ms)']
    },
    'spawnFirework': {
        signature: 'spawnFirework(position, config)',
        description: 'Spawn a firework at position',
        params: ['position: vec3', 'config: Object { colors, fadeColors, type, trail, flicker }']
    },

    // Cutscene functions
    'startCutscene': {
        signature: 'startCutscene(player, config)',
        description: 'Start a cinematic cutscene',
        params: ['player: Player', 'config: Object { disableMovement, hideHUD, waypoints, onComplete }']
    },
    'endCutscene': {
        signature: 'endCutscene(player)',
        description: 'End the current cutscene',
        params: ['player: Player']
    },

    // World functions
    'setBlock': {
        signature: 'setBlock(x, y, z, blockId)',
        description: 'Set a block in the world',
        params: ['x: Number', 'y: Number', 'z: Number', 'blockId: String']
    },
    'getBlock': {
        signature: 'getBlock(x, y, z)',
        description: 'Get the block at a position',
        params: ['x: Number', 'y: Number', 'z: Number']
    },
    'setWorldTime': {
        signature: 'setWorldTime(time)',
        description: 'Set the world time (0-24000)',
        params: ['time: Number (0=dawn, 6000=noon, 12000=dusk, 18000=midnight)']
    },
    'setWeather': {
        signature: 'setWeather(weather)',
        description: 'Set the weather',
        params: ['weather: "clear"|"rain"|"thunder"']
    },

    // Entity functions
    'spawnEntity': {
        signature: 'spawnEntity(entityType, position)',
        description: 'Spawn an entity at position',
        params: ['entityType: String', 'position: vec3']
    },
    'removeEntity': {
        signature: 'removeEntity(entityId)',
        description: 'Remove an entity from the world',
        params: ['entityId: String']
    },

    // Waypoint functions
    'createWaypoint': {
        signature: 'createWaypoint(player, waypointId, x, y, z)',
        description: 'Create a waypoint marker for a player',
        params: ['player: Player', 'waypointId: String', 'x: Number', 'y: Number', 'z: Number']
    },
    'removeWaypoint': {
        signature: 'removeWaypoint(player, waypointId)',
        description: 'Remove a waypoint marker',
        params: ['player: Player', 'waypointId: String']
    },

    // Scheduling functions
    'scheduleDelayed': {
        signature: 'scheduleDelayed(delay, callback, ...args)',
        description: 'Schedule a function to run after a delay',
        params: ['delay: Number (ms)', 'callback: Function', '...args: Any']
    },
    'scheduleRepeating': {
        signature: 'scheduleRepeating(interval, callback)',
        description: 'Schedule a function to run repeatedly',
        params: ['interval: Number (ms)', 'callback: Function']
    },
    'cancelSchedule': {
        signature: 'cancelSchedule(scheduleId)',
        description: 'Cancel a scheduled task',
        params: ['scheduleId: String']
    },

    // Utility functions
    'print': {
        signature: 'print(message)',
        description: 'Print a message to the console',
        params: ['message: String']
    },
    'vec3': {
        signature: 'vec3(x, y, z)',
        description: 'Create a 3D position vector',
        params: ['x: Number', 'y: Number', 'z: Number']
    },
    'random': {
        signature: 'random(min?, max?)',
        description: 'Generate a random number',
        params: ['min: Number (optional)', 'max: Number (optional)']
    }
};

// Keywords for completion
const KEYWORDS = [
    'var', 'function', 'if', 'else', 'for', 'while', 'return', 'in',
    'true', 'false', 'null', 'npc', 'dialogue', 'quest', 'on', 'node'
];

// Event types
const EVENTS = [
    'playerJoin', 'playerLeave', 'npcInteract', 'regionEnter', 'regionLeave',
    'entityDeath', 'questComplete', 'questStart', 'blockBreak', 'blockPlace',
    'itemPickup', 'itemDrop', 'chatMessage'
];

// Property names for declarations
const NPC_PROPERTIES = ['name', 'skin', 'position', 'dimension', 'invulnerable', 'health'];
const NODE_PROPERTIES = ['text', 'choices', 'next', 'action', 'condition', 'fallback'];
const QUEST_PROPERTIES = ['title', 'description', 'objectives', 'rewards', 'prerequisites', 'onComplete'];
const OBJECTIVE_PROPERTIES = ['id', 'description', 'type', 'target', 'count'];

export function activate(context: vscode.ExtensionContext) {
    console.log('Storykee Language Support is now active!');

    // Register completion provider
    const completionProvider = vscode.languages.registerCompletionItemProvider(
        'storykee',
        {
            provideCompletionItems(document: vscode.TextDocument, position: vscode.Position) {
                const linePrefix = document.lineAt(position).text.substring(0, position.character);
                const completions: vscode.CompletionItem[] = [];

                // Check context for property completions
                const inNpc = isInsideBlock(document, position, 'npc');
                const inDialogue = isInsideBlock(document, position, 'dialogue');
                const inNode = isInsideBlock(document, position, 'node');
                const inQuest = isInsideBlock(document, position, 'quest');
                const inObjective = isInsideBlock(document, position, 'objectives');

                // Property completions based on context
                if (inNpc && !inDialogue && !inQuest) {
                    NPC_PROPERTIES.forEach(prop => {
                        const item = new vscode.CompletionItem(prop, vscode.CompletionItemKind.Property);
                        item.insertText = new vscode.SnippetString(`${prop}: \${1};`);
                        completions.push(item);
                    });
                }

                if (inNode) {
                    NODE_PROPERTIES.forEach(prop => {
                        const item = new vscode.CompletionItem(prop, vscode.CompletionItemKind.Property);
                        if (prop === 'text') {
                            item.insertText = new vscode.SnippetString('text: "${1}";');
                        } else if (prop === 'choices') {
                            item.insertText = new vscode.SnippetString('choices: [\n\t{ text: "${1}", next: "${2}" }\n];');
                        } else {
                            item.insertText = new vscode.SnippetString(`${prop}: \${1};`);
                        }
                        completions.push(item);
                    });
                }

                if (inQuest && !inObjective) {
                    QUEST_PROPERTIES.forEach(prop => {
                        const item = new vscode.CompletionItem(prop, vscode.CompletionItemKind.Property);
                        completions.push(item);
                    });
                }

                if (inObjective) {
                    OBJECTIVE_PROPERTIES.forEach(prop => {
                        const item = new vscode.CompletionItem(prop, vscode.CompletionItemKind.Property);
                        completions.push(item);
                    });
                }

                // Event completions after 'on'
                if (linePrefix.match(/\bon\s+$/)) {
                    EVENTS.forEach(event => {
                        const item = new vscode.CompletionItem(event, vscode.CompletionItemKind.Event);
                        item.detail = 'Event handler';
                        completions.push(item);
                    });
                    return completions;
                }

                // Keyword completions
                KEYWORDS.forEach(keyword => {
                    const item = new vscode.CompletionItem(keyword, vscode.CompletionItemKind.Keyword);
                    completions.push(item);
                });

                // Function completions
                Object.entries(BUILTIN_FUNCTIONS).forEach(([name, info]) => {
                    const item = new vscode.CompletionItem(name, vscode.CompletionItemKind.Function);
                    item.detail = info.signature;
                    item.documentation = new vscode.MarkdownString(
                        `${info.description}\n\n**Parameters:**\n${info.params.map(p => `- ${p}`).join('\n')}`
                    );
                    completions.push(item);
                });

                return completions;
            }
        },
        ' ', '.', '('
    );

    // Register hover provider
    const hoverProvider = vscode.languages.registerHoverProvider('storykee', {
        provideHover(document: vscode.TextDocument, position: vscode.Position) {
            const range = document.getWordRangeAtPosition(position);
            if (!range) return;

            const word = document.getText(range);

            // Check if it's a built-in function
            if (BUILTIN_FUNCTIONS[word]) {
                const info = BUILTIN_FUNCTIONS[word];
                const markdown = new vscode.MarkdownString();
                markdown.appendCodeblock(info.signature, 'storykee');
                markdown.appendMarkdown(`\n\n${info.description}\n\n**Parameters:**\n`);
                info.params.forEach(param => {
                    markdown.appendMarkdown(`- \`${param}\`\n`);
                });
                return new vscode.Hover(markdown);
            }

            // Check if it's a keyword
            if (KEYWORDS.includes(word)) {
                return new vscode.Hover(new vscode.MarkdownString(`**${word}** - Storykee keyword`));
            }

            // Check if it's an event
            if (EVENTS.includes(word)) {
                return new vscode.Hover(new vscode.MarkdownString(`**${word}** - Event type\n\nUse with \`on ${word} { ... }\``));
            }

            return null;
        }
    });

    // Register signature help provider
    const signatureProvider = vscode.languages.registerSignatureHelpProvider(
        'storykee',
        {
            provideSignatureHelp(document: vscode.TextDocument, position: vscode.Position) {
                const lineText = document.lineAt(position).text;
                const textBeforeCursor = lineText.substring(0, position.character);

                // Find function name
                const match = textBeforeCursor.match(/(\w+)\s*\([^)]*$/);
                if (!match) return null;

                const funcName = match[1];
                const funcInfo = BUILTIN_FUNCTIONS[funcName];
                if (!funcInfo) return null;

                const signatureHelp = new vscode.SignatureHelp();
                const signature = new vscode.SignatureInformation(funcInfo.signature, funcInfo.description);
                
                funcInfo.params.forEach(param => {
                    signature.parameters.push(new vscode.ParameterInformation(param.split(':')[0].trim()));
                });

                signatureHelp.signatures = [signature];
                signatureHelp.activeSignature = 0;

                // Count commas to determine active parameter
                const paramsText = textBeforeCursor.substring(textBeforeCursor.lastIndexOf('(') + 1);
                const commaCount = (paramsText.match(/,/g) || []).length;
                signatureHelp.activeParameter = commaCount;

                return signatureHelp;
            }
        },
        '(', ','
    );

    // Register definition provider for go-to-definition
    const definitionProvider = vscode.languages.registerDefinitionProvider('storykee', {
        provideDefinition(document: vscode.TextDocument, position: vscode.Position) {
            const range = document.getWordRangeAtPosition(position);
            if (!range) return null;

            const word = document.getText(range);
            const text = document.getText();

            // Find NPC, dialogue, quest, or function definitions
            const patterns = [
                new RegExp(`\\bnpc\\s+${word}\\s*\\{`, 'g'),
                new RegExp(`\\bdialogue\\s+${word}\\s*\\{`, 'g'),
                new RegExp(`\\bquest\\s+${word}\\s*\\{`, 'g'),
                new RegExp(`\\bfunction\\s+${word}\\s*\\(`, 'g'),
                new RegExp(`\\bnode\\s+${word}\\s*\\{`, 'g')
            ];

            for (const pattern of patterns) {
                const match = pattern.exec(text);
                if (match) {
                    const pos = document.positionAt(match.index);
                    return new vscode.Location(document.uri, pos);
                }
            }

            return null;
        }
    });

    context.subscriptions.push(
        completionProvider,
        hoverProvider,
        signatureProvider,
        definitionProvider
    );
}

function isInsideBlock(document: vscode.TextDocument, position: vscode.Position, blockType: string): boolean {
    const text = document.getText(new vscode.Range(new vscode.Position(0, 0), position));
    
    // Simple brace counting to determine if we're inside a block
    const pattern = new RegExp(`\\b${blockType}\\b`, 'g');
    let lastMatch = -1;
    let match;
    
    while ((match = pattern.exec(text)) !== null) {
        lastMatch = match.index;
    }
    
    if (lastMatch === -1) return false;
    
    // Count braces after the last match
    const afterMatch = text.substring(lastMatch);
    let braceCount = 0;
    
    for (const char of afterMatch) {
        if (char === '{') braceCount++;
        if (char === '}') braceCount--;
    }
    
    return braceCount > 0;
}

export function deactivate() {}
