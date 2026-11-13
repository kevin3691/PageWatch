<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
const props = defineProps<{ modelValue: boolean; labelledby: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const dialog = ref<HTMLDialogElement>()
function sync() {
  if (props.modelValue && !dialog.value?.open) dialog.value?.showModal()
  else if (!props.modelValue && dialog.value?.open) dialog.value.close()
}
function outside(event: MouseEvent) {
  const rect = dialog.value?.getBoundingClientRect()
  if (
    rect &&
    (event.clientX < rect.left ||
      event.clientX > rect.right ||
      event.clientY < rect.top ||
      event.clientY > rect.bottom)
  )
    emit('update:modelValue', false)
}
onMounted(sync)
watch(() => props.modelValue, sync)
</script>

<template>
  <dialog
    ref="dialog"
    class="modal"
    :aria-labelledby="labelledby"
    @close="emit('update:modelValue', false)"
    @click="outside"
  >
    <slot />
  </dialog>
</template>
