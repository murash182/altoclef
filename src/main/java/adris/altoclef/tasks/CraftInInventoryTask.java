package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.resources.CollectRecipeCataloguedResourcesTask;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;

/**
 * Crafts an item within the 2x2 inventory crafting grid.
 */
public class CraftInInventoryTask extends ResourceTask {

    private final CraftingRecipe _recipe;
    private final boolean _collect;
    private final boolean _ignoreUncataloguedSlots;
    private boolean _fullCheckFailed = false;

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe, boolean collect, boolean ignoreUncataloguedSlots) {
        super(target);
        _recipe = recipe;
        _collect = collect;
        _ignoreUncataloguedSlots = ignoreUncataloguedSlots;
    }

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe) {
        this(target, recipe, true, false);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        _fullCheckFailed = false;
        StorageHelper.closeScreen(); // Just to be safe I guess
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // Grab from output FIRST
        if (StorageHelper.isPlayerInventoryOpen()) {
            if (StorageHelper.getItemStackInCursorSlot().isEmpty() && Arrays.stream(_itemTargets).anyMatch(target -> target.matches(StorageHelper.getItemStackInSlot(PlayerSlot.CRAFT_OUTPUT_SLOT).getItem()))) {
                return new ClickSlotTask(PlayerSlot.CRAFT_OUTPUT_SLOT, 0, SlotActionType.PICKUP);
            }
        }

        ItemTarget toGet = _itemTargets[0];
        if (_collect && !StorageHelper.hasRecipeMaterialsOrTarget(mod, new RecipeTarget(toGet, _recipe))) {
            // Collect recipe materials
            setDebugState("Collecting materials");
            return collectRecipeSubTask(mod);
        }

        // No need to free inventory, output gets picked up.

        setDebugState("Crafting in inventory... for " + toGet);
        return new CraftGenericTask(_recipe);
        //craftInstant(mod, _recipe);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CraftInInventoryTask task) {
            if (!task._recipe.equals(_recipe)) return false;
            return isCraftingEqual(task);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return toCraftingDebugStringName() + " " + _recipe;
    }

    // virtual. By default assumes subtasks are CATALOGUED (in TaskCatalogue.java)
    protected Task collectRecipeSubTask(AltoClef mod) {
        return new CollectRecipeCataloguedResourcesTask(_ignoreUncataloguedSlots, new RecipeTarget(_itemTargets[0], _recipe));
    }

    protected String toCraftingDebugStringName() {
        return "Craft 2x2 Task";
    }

    protected boolean isCraftingEqual(CraftInInventoryTask other) {
        return true;
    }
}
