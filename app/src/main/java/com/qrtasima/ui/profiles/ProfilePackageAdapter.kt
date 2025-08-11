package com.qrtasima.ui.profiles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qrtasima.data.ProfilePackage
import com.qrtasima.databinding.ItemProfilePackageBinding

class ProfilePackageAdapter(
    private val onPackageClicked: (ProfilePackage) -> Unit,
    private val onQrClicked: (ProfilePackage) -> Unit,
    private val onDeleteClicked: (ProfilePackage) -> Unit,
    private val onEditClicked: (ProfilePackage) -> Unit
) : ListAdapter<ProfilePackage, ProfilePackageAdapter.PackageViewHolder>(PackagesComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = ItemProfilePackageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackageViewHolder(binding, onPackageClicked, onQrClicked, onDeleteClicked, onEditClicked)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PackageViewHolder(
        private val binding: ItemProfilePackageBinding,
        private val onPackageClicked: (ProfilePackage) -> Unit,
        private val onQrClicked: (ProfilePackage) -> Unit,
        private val onDeleteClicked: (ProfilePackage) -> Unit,
        private val onEditClicked: (ProfilePackage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfilePackage) {
            binding.textViewPackageName.text = item.name
            binding.root.setOnClickListener { onPackageClicked(item) }
            binding.buttonShowQr.setOnClickListener { onQrClicked(item) }
            binding.buttonDeletePackage.setOnClickListener { onDeleteClicked(item) }
            binding.buttonEditPackage.setOnClickListener { onEditClicked(item) }
        }
    }

    class PackagesComparator : DiffUtil.ItemCallback<ProfilePackage>() {
        override fun areItemsTheSame(o: ProfilePackage, n: ProfilePackage) = o.id == n.id
        override fun areContentsTheSame(o: ProfilePackage, n: ProfilePackage) = o == n
    }
}