package adris.altoclef.tasks.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

public class FillStrongholdPortalTask extends Task {

    private final boolean _destroySilverfishSpawner;

    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10);
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker(3);

    public FillStrongholdPortalTask(boolean destroySilverfishSpawner) {
        _destroySilverfishSpawner = destroySilverfishSpawner;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().setPreferredStairs(false);
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL_FRAME, Blocks.END_PORTAL);
        if (_destroySilverfishSpawner) {
            mod.getBlockTracker().trackBlock(Blocks.SPAWNER);
        }
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // If we encounter that weird back+forth bug, this might fix.
        // Overkill, but it would REALLY suck if the run stopped here.
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            _progressChecker.reset();
            setDebugState("Wandering");
            return _wanderTask;
        }

        if (_destroySilverfishSpawner) {
            Optional<BlockPos> silverfishSpawner = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), test -> (WorldHelper.getSpawnerEntity(mod, test) instanceof SilverfishEntity), Blocks.SPAWNER);
            if (silverfishSpawner.isPresent()) {
                setDebugState("Destroy silverfish spawner");
                return new DestroyBlockTask(silverfishSpawner.get());
            }
        }

        setDebugState("Filling in Portal");
        if (!_progressChecker.check(mod)) {
            _progressChecker.reset();
            return _wanderTask;
        }
        return new DoToClosestBlockTask(
                pos -> new InteractWithBlockTask(new ItemTarget(Items.ENDER_EYE, 1), Direction.UP, pos, true),
                test -> !isEndPortalFrameFilled(mod, test),
                Blocks.END_PORTAL_FRAME
        );
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL_FRAME, Blocks.END_PORTAL);
        if (_destroySilverfishSpawner) {
            mod.getBlockTracker().stopTracking(Blocks.SPAWNER);
        }
        mod.getBehaviour().pop();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        Optional<BlockPos> closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), Blocks.END_PORTAL);
        return closest.isPresent() && mod.getChunkTracker().isChunkLoaded(closest.get());
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof FillStrongholdPortalTask) {
            return ((FillStrongholdPortalTask) other)._destroySilverfishSpawner == _destroySilverfishSpawner;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Fill Stronghold Portal";
    }

    private static boolean isEndPortalFrameFilled(AltoClef mod, BlockPos pos) {
        if (!mod.getChunkTracker().isChunkLoaded(pos)) return false;
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.getBlock() != Blocks.END_PORTAL_FRAME) {
            Debug.logWarning("BLOCK POS " + pos + " DOES NOT CONTAIN END PORTAL FRAME! This is probably due to a bug/incorrect assumption.");
        }
        return state.get(EndPortalFrameBlock.EYE);
    }
}
