# Storykee Language Support for VS Code

Расширение для VS Code, обеспечивающее поддержку языка Storykee (`.skee`) для мода It's My Story.

## Возможности

### Подсветка синтаксиса
- Ключевые слова (`var`, `function`, `if`, `else`, `for`, `while`, `return`)
- Декларации (`npc`, `dialogue`, `quest`, `node`, `on`)
- Строки и числа
- Комментарии (однострочные `//` и многострочные `/* */`)
- Встроенные функции
- Java-секции

### Автодополнение (IntelliSense)
- Встроенные функции с документацией
- Ключевые слова языка
- Типы событий (`playerJoin`, `npcInteract`, и др.)
- Свойства для NPC, диалогов, квестов
- Контекстно-зависимые подсказки

### Сниппеты
- `npc` - создание NPC
- `dialogue` - создание диалога
- `quest` - создание квеста
- `func` - объявление функции
- `on-join` - обработчик входа игрока
- `on-npc` - обработчик взаимодействия с NPC
- И многие другие...

### Дополнительные функции
- Подсказки при наведении (Hover)
- Подсказки параметров функций (Signature Help)
- Переход к определению (Go to Definition)
- Автоматическое закрытие скобок и кавычек
- Сворачивание блоков кода

## Установка

### Из VSIX файла
1. Скачайте файл `storykee-language-x.x.x.vsix`
2. В VS Code: Extensions → ... → Install from VSIX
3. Выберите скачанный файл

### Для разработки
```bash
cd misc/vscode-ext
npm install
npm run compile
```

Затем нажмите F5 для запуска Extension Development Host.

## Использование

1. Создайте файл с расширением `.skee`
2. Начните писать код - подсветка и автодополнение работают автоматически
3. Используйте сниппеты для быстрого создания структур

### Примеры сниппетов

Введите `npc` и нажмите Tab:
```storykee
npc myNpc {
    name: "NPC Name";
    skin: "skin_name";
    position: vec3(0, 64, 0);
    invulnerable: true;
}
```

Введите `dialogue` и нажмите Tab:
```storykee
dialogue myDialogue {
    speaker: "Speaker Name";
    
    node start {
        text: "Hello!";
        choices: [
            { text: "Option 1", next: "node1" },
            { text: "Goodbye", next: "end" }
        ];
    }
    
    node node1 {
        text: "Response";
    }
}
```

## Сборка VSIX

```bash
npm install -g @vscode/vsce
cd misc/vscode-ext
npm install
npm run compile
vsce package
```

## Структура расширения

```
misc/vscode-ext/
├── package.json              # Манифест расширения
├── language-configuration.json # Настройки языка
├── tsconfig.json             # Конфигурация TypeScript
├── src/
│   └── extension.ts          # Основной код расширения
├── syntaxes/
│   └── storykee.tmLanguage.json # Грамматика для подсветки
└── snippets/
    └── storykee.json         # Сниппеты
```

## Лицензия

MIT
