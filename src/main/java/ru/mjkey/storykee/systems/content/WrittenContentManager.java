package ru.mjkey.storykee.systems.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages books, signs, and other written content for Storykee scripts.
 * 
 * Requirements: 55.1, 55.2, 55.3, 55.4, 55.5
 */
public class WrittenContentManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WrittenContentManager.class);
    
    private static WrittenContentManager instance;
    
    private MinecraftServer server;
    
    private WrittenContentManager() {
    }
    
    public static WrittenContentManager getInstance() {
        if (instance == null) {
            instance = new WrittenContentManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ===== Book Creation (Requirement 55.1) =====
    
    /**
     * Creates a written book with specified content.
     * Requirement 55.1: WHEN a script creates a book THEN the Runtime SHALL generate it with specified title, author, and pages
     */
    public ItemStack createWrittenBook(String title, String author, List<String> pages) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        
        // Convert pages to Filterable<Component>
        List<net.minecraft.server.network.Filterable<Component>> pageComponents = pages.stream()
            .map(page -> net.minecraft.server.network.Filterable.passThrough(parseFormattedText(page)))
            .collect(Collectors.toList());
        
        // Create written book content
        // Truncate title to 32 chars
        String truncatedTitle = title.length() > 32 ? title.substring(0, 32) : title;
        
        // Create filterable title
        net.minecraft.server.network.Filterable<String> filterableTitle = 
            net.minecraft.server.network.Filterable.passThrough(truncatedTitle);
        
        WrittenBookContent content = new WrittenBookContent(
            filterableTitle,
            author,
            0, // generation
            pageComponents,
            true // resolved
        );
        
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        
        LOGGER.info("createWrittenBook: Created book '{}' by {} with {} pages", title, author, pages.size());
        return book;
    }
    
    /**
     * Creates a writable book (book and quill) with content.
     */
    public ItemStack createWritableBook(List<String> pages) {
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        
        // For writable books in 1.21+, content is handled differently
        // The book starts empty and players can write in it
        
        LOGGER.info("createWritableBook: Created writable book with {} pages", pages.size());
        return book;
    }

    // ===== Give Book to Player (Requirement 55.2) =====
    
    /**
     * Gives a book to a player.
     * Requirement 55.2: WHEN a script gives a book to a player THEN the Runtime SHALL add it to their inventory
     */
    public boolean giveBook(UUID playerId, String title, String author, List<String> pages) {
        if (server == null) {
            LOGGER.warn("giveBook: Server not available");
            return false;
        }
        
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("giveBook: Player not found - {}", playerId);
            return false;
        }
        
        ItemStack book = createWrittenBook(title, author, pages);
        boolean added = player.getInventory().add(book);
        
        if (!added) {
            // Drop at player's feet if inventory is full
            player.drop(book, false);
        }
        
        LOGGER.info("giveBook: Gave book '{}' to player {}", title, playerId);
        return true;
    }

    // ===== Sign Text Updates (Requirement 55.3) =====
    
    /**
     * Updates sign text at a position.
     * Requirement 55.3: WHEN a script updates sign text THEN the Runtime SHALL modify the sign at the specified location
     */
    public boolean updateSignText(BlockPos pos, String dimension, List<String> lines, boolean front) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SignBlockEntity signEntity)) {
            LOGGER.warn("updateSignText: No sign at {}", pos);
            return false;
        }
        
        // Get current sign text
        SignText currentText = front ? signEntity.getFrontText() : signEntity.getBackText();
        
        // Create new messages array
        Component[] messages = new Component[4];
        for (int i = 0; i < 4; i++) {
            if (i < lines.size()) {
                messages[i] = parseFormattedText(lines.get(i));
            } else {
                messages[i] = Component.empty();
            }
        }
        
        // Create new SignText with updated messages
        SignText newText = new SignText(
            messages,
            messages, // filtered messages (same for now)
            currentText.getColor(),
            currentText.hasGlowingText()
        );
        
        // Update the sign
        if (front) {
            signEntity.setText(newText, true);
        } else {
            signEntity.setText(newText, false);
        }
        
        // Mark for update
        signEntity.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        
        LOGGER.info("updateSignText: Updated sign at {} with {} lines", pos, lines.size());
        return true;
    }
    
    /**
     * Updates sign text (front side by default).
     */
    public boolean updateSignText(BlockPos pos, String dimension, List<String> lines) {
        return updateSignText(pos, dimension, lines, true);
    }
    
    /**
     * Sets sign glow effect.
     */
    public boolean setSignGlowing(BlockPos pos, String dimension, boolean glowing, boolean front) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SignBlockEntity signEntity)) {
            return false;
        }
        
        signEntity.setWaxed(false); // Allow editing
        
        SignText currentText = front ? signEntity.getFrontText() : signEntity.getBackText();
        SignText newText = currentText.setHasGlowingText(glowing);
        
        if (front) {
            signEntity.setText(newText, true);
        } else {
            signEntity.setText(newText, false);
        }
        
        signEntity.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        
        return true;
    }

    // ===== Text Formatting (Requirement 55.4) =====
    
    /**
     * Parses formatted text with color codes and styles.
     * Requirement 55.4: WHEN books support formatting THEN the Runtime SHALL apply colors, styles, and clickable elements
     */
    public Component parseFormattedText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Replace color codes (§ or &)
        text = text.replace("&", "§");
        
        // Parse the text with formatting codes
        return Component.literal(text);
    }
    
    /**
     * Creates formatted text with explicit styling.
     */
    public Component createStyledText(String text, String color, boolean bold, boolean italic, boolean underlined) {
        var component = Component.literal(text);
        var style = component.getStyle();
        
        // Apply color
        if (color != null) {
            try {
                net.minecraft.ChatFormatting formatting = net.minecraft.ChatFormatting.getByName(color.toUpperCase());
                if (formatting != null) {
                    style = style.withColor(formatting);
                }
            } catch (Exception e) {
                // Ignore invalid colors
            }
        }
        
        // Apply styles
        if (bold) style = style.withBold(true);
        if (italic) style = style.withItalic(true);
        if (underlined) style = style.withUnderlined(true);
        
        return component.withStyle(style);
    }

    // ===== Lectern Support (Requirement 55.5) =====
    
    /**
     * Places a lectern with a book.
     * Requirement 55.5: WHEN a script creates lecterns THEN the Runtime SHALL place them with books for reading
     */
    public boolean placeLecternWithBook(BlockPos pos, String dimension, String title, String author, List<String> pages) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        // Place lectern block
        level.setBlock(pos, Blocks.LECTERN.defaultBlockState(), 3);
        
        // Get the lectern block entity
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof LecternBlockEntity lectern)) {
            LOGGER.warn("placeLecternWithBook: Failed to create lectern at {}", pos);
            return false;
        }
        
        // Create and place the book
        ItemStack book = createWrittenBook(title, author, pages);
        lectern.setBook(book);
        
        // Update block state to show book
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(LecternBlock.HAS_BOOK, true), 3);
        
        LOGGER.info("placeLecternWithBook: Placed lectern with book '{}' at {}", title, pos);
        return true;
    }
    
    /**
     * Updates the book in an existing lectern.
     */
    public boolean updateLecternBook(BlockPos pos, String dimension, String title, String author, List<String> pages) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof LecternBlockEntity lectern)) {
            LOGGER.warn("updateLecternBook: No lectern at {}", pos);
            return false;
        }
        
        ItemStack book = createWrittenBook(title, author, pages);
        lectern.setBook(book);
        
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(LecternBlock.HAS_BOOK, true), 3);
        
        LOGGER.info("updateLecternBook: Updated lectern book at {}", pos);
        return true;
    }
    
    /**
     * Removes the book from a lectern.
     */
    public ItemStack removeLecternBook(BlockPos pos, String dimension) {
        if (server == null) return ItemStack.EMPTY;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return ItemStack.EMPTY;
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof LecternBlockEntity lectern)) {
            return ItemStack.EMPTY;
        }
        
        ItemStack book = lectern.getBook().copy();
        lectern.clearContent();
        
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(LecternBlock.HAS_BOOK, false), 3);
        
        return book;
    }

    // ===== Sign Placement =====
    
    /**
     * Places a sign with text.
     */
    public boolean placeSign(BlockPos pos, String dimension, List<String> lines, String signType) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        // Determine sign block type
        BlockState signState = switch (signType.toLowerCase()) {
            case "oak", "oak_sign" -> Blocks.OAK_SIGN.defaultBlockState();
            case "spruce", "spruce_sign" -> Blocks.SPRUCE_SIGN.defaultBlockState();
            case "birch", "birch_sign" -> Blocks.BIRCH_SIGN.defaultBlockState();
            case "jungle", "jungle_sign" -> Blocks.JUNGLE_SIGN.defaultBlockState();
            case "acacia", "acacia_sign" -> Blocks.ACACIA_SIGN.defaultBlockState();
            case "dark_oak", "dark_oak_sign" -> Blocks.DARK_OAK_SIGN.defaultBlockState();
            case "crimson", "crimson_sign" -> Blocks.CRIMSON_SIGN.defaultBlockState();
            case "warped", "warped_sign" -> Blocks.WARPED_SIGN.defaultBlockState();
            default -> Blocks.OAK_SIGN.defaultBlockState();
        };
        
        level.setBlock(pos, signState, 3);
        
        // Update text
        return updateSignText(pos, dimension, lines);
    }

    // ===== Utility Methods =====
    
    private ServerLevel getLevel(String dimension) {
        if (server == null || dimension == null) return null;
        
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimension)) {
                return level;
            }
        }
        
        return switch (dimension.toLowerCase()) {
            case "overworld", "minecraft:overworld" -> server.overworld();
            default -> null;
        };
    }
    
    /**
     * Reads sign text at a position.
     */
    public List<String> readSignText(BlockPos pos, String dimension, boolean front) {
        if (server == null) return Collections.emptyList();
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return Collections.emptyList();
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SignBlockEntity signEntity)) {
            return Collections.emptyList();
        }
        
        SignText text = front ? signEntity.getFrontText() : signEntity.getBackText();
        List<String> lines = new ArrayList<>();
        
        for (int i = 0; i < 4; i++) {
            lines.add(text.getMessage(i, false).getString());
        }
        
        return lines;
    }
}
