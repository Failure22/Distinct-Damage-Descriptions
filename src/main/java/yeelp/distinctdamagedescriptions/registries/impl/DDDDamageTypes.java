package yeelp.distinctdamagedescriptions.registries.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.projectile.EntityEvokerFangs;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import yeelp.distinctdamagedescriptions.DistinctDamageDescriptions;
import yeelp.distinctdamagedescriptions.ModConfig;
import yeelp.distinctdamagedescriptions.capability.DamageDistribution;
import yeelp.distinctdamagedescriptions.capability.IDamageDistribution;
import yeelp.distinctdamagedescriptions.registries.IDDDDamageTypeRegistry;
import yeelp.distinctdamagedescriptions.util.DDDBuiltInDamageType;
import yeelp.distinctdamagedescriptions.util.DDDConfigReader;
import yeelp.distinctdamagedescriptions.util.DDDDamageType;
import yeelp.distinctdamagedescriptions.util.DamageTypeData;
import yeelp.distinctdamagedescriptions.util.lib.NonNullMap;
import yeelp.distinctdamagedescriptions.util.lib.YResources;

public final class DDDDamageTypes extends DDDBaseRegistry<DDDDamageType> implements IDDDDamageTypeRegistry
{
	private DataMap dataMap = new DataMap();
	private ExtraMap extraMap = new ExtraMap();
	
	private final class DataMap
	{
		private final Map<String, DDDDamageType> includeAllMap;
		private final Map<String, SourceMap> srcMap;
		
		protected DataMap()
		{
			this.includeAllMap = new NonNullMap<String, DDDDamageType>(DDDBuiltInDamageType.NORMAL);
			this.srcMap = new HashMap<String, SourceMap>();
		}
		
		final class SourceMap
		{
			private final Map<String, DDDDamageType> direct, indirect;
			SourceMap()
			{
				this(DDDBuiltInDamageType.NORMAL);
			}
			
			SourceMap(DDDDamageType defaultVal)
			{
				this.direct = new NonNullMap<String, DDDDamageType>(defaultVal);
				this.indirect = new NonNullMap<String, DDDDamageType>(defaultVal);
			}
			
			void update(DDDDamageType type, DamageTypeData data)
			{
				for(String s : data.getDirectSources())
				{
					this.direct.put(s, type);
				}
				for(String s : data.getIndirectSources())
				{
					this.indirect.put(s, type);
				}
			}
		}
		
		void update(DDDDamageType type, DamageTypeData data)
		{
			if(data.includeAll())
			{
				this.includeAllMap.put(data.getOriginalSource(), type);
			}
			if(this.srcMap.containsKey(data.getOriginalSource())) //this.srcMap.putIfAbsent would still require the compute a new SourceMap every time, so it's less efficient
			{
				this.srcMap.put(data.getOriginalSource(), new SourceMap());
			}
			this.srcMap.get(data.getOriginalSource()).update(type, data);
		}
	}
	
	public DDDDamageTypes()
	{
		super(d -> d.getTypeName());
	}
	
	@Override
	public void init()
	{
		this.registerAll(DDDBuiltInDamageType.ACID, 
						 DDDBuiltInDamageType.BLUDGEONING, 
						 DDDBuiltInDamageType.COLD, 
						 DDDBuiltInDamageType.FIRE, 
						 DDDBuiltInDamageType.FORCE, 
						 DDDBuiltInDamageType.LIGHTNING, 
						 DDDBuiltInDamageType.NECROTIC, 
						 DDDBuiltInDamageType.NORMAL, 
						 DDDBuiltInDamageType.PIERCING, 
						 DDDBuiltInDamageType.POISON, 
						 DDDBuiltInDamageType.PSYCHIC, 
						 DDDBuiltInDamageType.RADIANT, 
						 DDDBuiltInDamageType.SLASHING, 
						 DDDBuiltInDamageType.THUNDER, 
						 DDDBuiltInDamageType.UNKNOWN);
	}
	
	@Override
	public void registerDamageTypeData(DDDDamageType type, DamageTypeData... datas)
	{
		for(DamageTypeData d : datas)
		{
			this.dataMap.update(type, d);
		}
	}
	
	@Override
	public Set<DDDDamageType> getCustomDamageContext(LivingAttackEvent evt)
	{
		DamageSource src = evt.getSource();
		HashSet<DDDDamageType> set = new HashSet<DDDDamageType>();
		set.add(dataMap.includeAllMap.get(src.getDamageType()));
		if(dataMap.srcMap.containsKey(src.getDamageType()))
		{
			Optional<String> direct = YResources.getEntityIDString(src.getImmediateSource());
			Optional<String> indirect = YResources.getEntityIDString(src.getTrueSource());
			DistinctDamageDescriptions.debug(direct.orElse("")+", "+indirect.orElse(""));
			DataMap.SourceMap sMap = dataMap.srcMap.get(src.getDamageType());
			DDDDamageType directType = sMap.direct.get(direct.orElse(""));
			DDDDamageType indirectType = sMap.indirect.get(indirect.orElse(""));
			DistinctDamageDescriptions.debug(directType.getTypeName() +", "+indirectType.getTypeName());
			set.add(directType);
			set.add(indirectType);
		}
		return set;
	}
	
	@Override
	public Set<DDDDamageType> getExtraDamageContext(LivingAttackEvent evt)
	{
		return extraMap.get(evt);
	}
	@Override
	public Set<DDDDamageType> getRegularDamageContext(LivingAttackEvent evt)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void updateExplosionDamage()
	{
		
	}
}
