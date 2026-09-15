package com.aura.launcher.launcher.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.customization.icons.IconShape
import com.aura.launcher.customization.icons.IconStylePack
import com.aura.launcher.customization.vibesync.ExtractedVibePalette
import com.aura.launcher.customization.widgets.AuraWidgetInfo
import com.aura.launcher.customization.widgets.WidgetType
import com.aura.launcher.domain.model.Folder
import com.aura.launcher.domain.model.HomeItem
import com.aura.launcher.domain.model.HomeItemType
import com.aura.launcher.domain.usecase.GetInstalledAppsUseCase
import com.aura.launcher.domain.usecase.LaunchAppUseCase
import com.aura.launcher.domain.usecase.ManageHomeItemsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val manageHomeItemsUseCase: ManageHomeItemsUseCase,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val launchAppUseCase: LaunchAppUseCase
) : ViewModel() {

    val page0Items: StateFlow<List<HomeItem>> = manageHomeItemsUseCase.getPageItems(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All items across every page — used to drive the swipeable multi-page home surface
    val allHomeItems: StateFlow<List<HomeItem>> = manageHomeItemsUseCase.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Matches css .launcher-page-dots: always one blank page available past the last used page
    val pageCount: StateFlow<Int> = allHomeItems.map { items ->
        val maxUsedPage = items.filter { it.pageIndex >= 0 }.maxOfOrNull { it.pageIndex } ?: 0
        (maxUsedPage + 2).coerceAtLeast(1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val _activePageIndex = MutableStateFlow(0)
    val activePageIndex: StateFlow<Int> = _activePageIndex.asStateFlow()

    fun setActivePage(index: Int) {
        _activePageIndex.value = index
    }

    val dockItems: StateFlow<List<HomeItem>> = manageHomeItemsUseCase.getDockItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeIconShape = MutableStateFlow(IconShape.SQUIRCLE)
    val activeIconShape: StateFlow<IconShape> = _activeIconShape.asStateFlow()

    private val _activeIconPack = MutableStateFlow(IconStylePack.DEFAULT)
    val activeIconPack: StateFlow<IconStylePack> = _activeIconPack.asStateFlow()

    private val _vibePalette = MutableStateFlow(ExtractedVibePalette())
    val vibePalette: StateFlow<ExtractedVibePalette> = _vibePalette.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = getInstalledAppsUseCase().first()
            if (apps.isNotEmpty()) {
                manageHomeItemsUseCase.initializeDefault(apps)
            }
        }
    }

    fun addWidgetToHome(widget: AuraWidgetInfo, pageIndex: Int = 0) {
        viewModelScope.launch {
            val item = HomeItem(
                pageIndex = pageIndex,
                cellX = 0,
                cellY = 0,
                spanX = widget.spanX,
                spanY = widget.spanY,
                itemType = HomeItemType.WIDGET,
                label = widget.title,
                widgetProvider = widget.type.name
            )
            manageHomeItemsUseCase.addItem(item)
        }
    }

    fun setIconShape(shape: IconShape) {
        _activeIconShape.value = shape
    }

    fun setIconPack(pack: IconStylePack) {
        _activeIconPack.value = pack
    }

    fun applyVibePalette(palette: ExtractedVibePalette) {
        _vibePalette.value = palette
    }

    private val _activeFolder = MutableStateFlow<Folder?>(null)
    val activeFolder: StateFlow<Folder?> = _activeFolder.asStateFlow()

    fun openFolder(folderId: Long) {
        viewModelScope.launch {
            val folder = manageHomeItemsUseCase.getFolder(folderId)
            _activeFolder.value = folder
        }
    }

    fun closeFolder() {
        _activeFolder.value = null
    }

    fun renameFolder(folderId: Long, newTitle: String) {
        viewModelScope.launch {
            val current = _activeFolder.value ?: return@launch
            val appKeys = current.apps.map { it.componentKey }
            manageHomeItemsUseCase.updateFolder(folderId, newTitle, appKeys)
            _activeFolder.value = current.copy(title = newTitle)

            // Also update the HomeItem label
            val all = allHomeItems.value
            val folderItem = all.firstOrNull { it.folderId == folderId }
            if (folderItem != null) {
                manageHomeItemsUseCase.updateItem(folderItem.copy(label = newTitle))
            }
        }
    }

    fun createFolderWithItems(title: String, item1: HomeItem, item2: HomeItem, pageIndex: Int) {
        viewModelScope.launch {
            val keys = mutableListOf<String>()
            item1.packageName?.let { pkg ->
                item1.activityName?.let { act -> keys.add("$pkg/$act") }
            }
            item2.packageName?.let { pkg ->
                item2.activityName?.let { act -> keys.add("$pkg/$act") }
            }

            val folderId = manageHomeItemsUseCase.createFolder(title, "#7000FF", keys)

            // Remove original items and insert folder item
            manageHomeItemsUseCase.removeItem(item1.id)
            manageHomeItemsUseCase.removeItem(item2.id)

            val folderHomeItem = HomeItem(
                pageIndex = pageIndex,
                cellX = item1.cellX,
                cellY = item1.cellY,
                itemType = HomeItemType.FOLDER,
                label = title,
                folderId = folderId
            )
            manageHomeItemsUseCase.addItem(folderHomeItem)
        }
    }

    fun moveHomeItem(itemId: Long, targetPageIndex: Int, targetCellX: Int, targetCellY: Int) {
        viewModelScope.launch {
            val all = allHomeItems.value
            val target = all.firstOrNull { it.id == itemId } ?: return@launch
            manageHomeItemsUseCase.updateItem(
                target.copy(
                    pageIndex = targetPageIndex,
                    cellX = targetCellX,
                    cellY = targetCellY
                )
            )
        }
    }

    fun launchHomeItem(item: HomeItem) {
        if (item.itemType == HomeItemType.FOLDER && item.folderId != null) {
            openFolder(item.folderId)
            return
        }
        val pkg = item.packageName ?: return
        viewModelScope.launch {
            launchAppUseCase(pkg, item.activityName)
        }
    }

    fun removeHomeItem(item: HomeItem) {
        viewModelScope.launch {
            manageHomeItemsUseCase.removeItem(item.id)
            if (item.folderId != null) {
                manageHomeItemsUseCase.deleteFolder(item.folderId)
            }
        }
    }
}